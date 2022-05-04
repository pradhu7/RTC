import apxapi
import xml.etree.ElementTree as ET
from PIL import Image
from io import BytesIO
from argparse import ArgumentParser
from concurrent.futures import ThreadPoolExecutor

class PageOCRComparer():
  def __init__(self,session):
    self.session = session
    self.dtd = open('parse_html.dtd').read()

  def evaluate_document_ocr(self, doc_id, min_percent=1):
    ratios = [v['image_aspect_ratio']/v['html_aspect_ratio']
              for (k, v) in self.collect_page_ratios(doc_id).items()]
    if any(r > 1 for r in ratios):
        return False
    return min(ratios) >= min_percent

  def min_document_ratio(self, doc_id):
    ratios = [v['image_aspect_ratio']/v['html_aspect_ratio']
              for (k, v) in self.collect_page_ratios(doc_id).items()]
    return min(ratios)

  def collect_page_ratios(self, doc_id):
    patient = self.session.dataorchestrator.patient_object_by_doc(doc_id).json()
    document = patient['documents'][0]
    mime_type = document['documentContents'][0]['mimeType'] if len(document['documentContents']) > 0 else 'Unknown'
    if mime_type != 'application/pdf':
      print('Non-PDF document')
      return False

    string_content = document['stringContent']
    if string_content.startswith('<!DOCTYPE'):
      string_content = self.dtd + string_content[string_content.index('>')+1:]
    root = ET.fromstring(string_content)

    with ThreadPoolExecutor() as tp:
        return {ret['page_number']: ret
                for ret in tp.map(lambda page: self.compare_image_and_html_ratios(doc_id, page),
                                  root.findall('pages/page'))}

  def compare_image_and_html_ratios(self, doc_id, page):
    page_number = page.find('pageNumber').text
    extracted_text = page.find('extractedText/content').text
    plain_text = page.find('plainText').text
    extraction_type = None
    if extracted_text:
      extraction_type = 'hocr'
    elif plain_text:
      extraction_type = 'html'
    else:
      print('Error! No plain or extracted text to process')
      return False
    html_width, html_height = self.get_hocr_dimensions(extracted_text) if extracted_text else self.get_text_dimensions(plain_text)
    if not html_height and not html_width:
      print('Could not get dimensions from page HTML')
      return False
    html_aspect_ratio = round(html_width / html_height, 5)
    # Since we got a ratio from the HTML content, let's compare it to the image we have in S3 (0-indexed)
    page_image_bytes = self.session.dataorchestrator.page(doc_id, int(page_number) - 1).content
    page_image = Image.open(BytesIO(page_image_bytes))
    image_width, image_height = page_image.size
    image_aspect_ratio = round(image_width / image_height, 5)
    rotated = False
    ocr_height_doubled = False
    matching_ratio = image_aspect_ratio == html_aspect_ratio
    if not matching_ratio:
      rotated_image_aspect_ratio = round(image_height / image_width, 5)
      matching_ratio = rotated_image_aspect_ratio == html_aspect_ratio
      if matching_ratio:
        rotated = True
      else:
        doubled_height_html_aspect_ratio = round(html_width / (2 * html_height), 5)
        matching_ratio = image_aspect_ratio == doubled_height_html_aspect_ratio
        ocr_height_doubled = True

    print('Page %s, %s - Image and HTML ratios match? %s, Rotated? %s, OCR Height Doubled? %s (Image: %s / %s / %s, HTML: %s / %s / %s)' %
      (page_number, extraction_type, matching_ratio, rotated, ocr_height_doubled, image_width, image_height, image_aspect_ratio, html_width, html_height, html_aspect_ratio))
#     return matching_ratio
    return {'page_number': page_number,
            'extraction_type': extraction_type,
            'matching_ratio': matching_ratio,
            'rotated': rotated,
            'ocr_height_doubled': ocr_height_doubled,
            'image_width': image_width,
            'image_height': image_height,
            'image_aspect_ratio': image_aspect_ratio,
            'html_width': html_width,
            'html_height': html_height,
            'html_aspect_ratio': html_aspect_ratio}

  def get_hocr_dimensions(self, extracted_text):
    bad_tags = [
      '<meta http-equiv="Content-Type" content="text/html;charset=utf-8">',
      '<meta name="ocr-system" content="tesseract">'
    ]
    cleaned_html = extracted_text
    for bad_tag in bad_tags:
      cleaned_html = cleaned_html.replace(bad_tag,'')
    cleaned_html = cleaned_html.replace('&?','') # Not sure if this is right but works for this doc
    if cleaned_html.startswith('<!DOCTYPE'):
      cleaned_html = self.dtd + cleaned_html[cleaned_html.index('>')+1:]
    root = ET.fromstring(cleaned_html)
    if root.tag == 'div' and root.attrib['class'] == 'ocr_page':
      details = root.attrib['title'].split(';')
    else:
      details = root.find('.//div[@class="ocr_page"]').get('title').split(';')
    height = 0
    width = 0
    for detail in details:
      if detail.strip().startswith('bbox'):
        coordinates = [detail.replace(';','') for detail in detail.strip().split(" ")[1:]]
        width = int(coordinates[2])
        height = int(coordinates[3])
    return (width, height)

  def get_text_dimensions(self, plain_text):
    root = ET.fromstring('%s<plainText>%s</plainText>' % (self.dtd,plain_text))
    page_style = root.find('.//div[@class="page"]').attrib['style']
    details = {s.split(':')[0]:s.split(':')[1] for s in page_style.split(';') if len(s.split(':')) > 1}
    height = float(details['height'].replace('pt',''))
    width = float(details['width'].replace('pt',''))
    return (width, height)

# Test Cases:
# '07d73a52-1de1-424e-8cf5-e891f1d25747': HOCR, aspect ratio different and highlighting does NOT work
# '4583a283-24a2-47d3-afff-14723db5afd7': HTML, aspect ratio matches
# '7947da81-b89d-442b-b650-531959b1902b': HOCR, aspect ratio different but highlighting still works because the height for ocr_page and all words is simply scaled by 0.5
if __name__ == "__main__":
  parser = ArgumentParser()
  parser.add_argument("-d", "--document",help="Single document to compare page ratios.")
  parser.add_argument("-u", "--username",help="Username for apxapi session.")

  args = parser.parse_args()
  username = args.username if args.username else input('Username: ')
  session = apxapi.APXSession(username, environment=apxapi.PRD)
  comparer = PageOCRComparer(session)
  comparer.evaluate_document_ocr(args.document)
