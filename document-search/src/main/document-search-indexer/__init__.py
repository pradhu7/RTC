# coding=utf-8
import apxapi
from apxapi.threadpool import ThreadPool
import xml.etree.ElementTree as ET
from PIL import ImageFont
from pysqlcipher3 import dbapi2 as sqlcipher
from mysql import connector
import getpass
import os
import sys
import traceback
import re
import datetime
import getpass
from argparse import ArgumentParser
import getpass

# TODO: delete bad data if indexer is updated?

class PatientSearchIndexer():
  def __init__(self,
               session,
               font_dir,
               db_dir,
               force,
               skip_partial,
               page_completeness,
               document_completenes):
    self.session = session
    self.font_dir = font_dir
    self.db_dir = db_dir
    self.dtd = open('resources/parse_html.dtd').read()
    self.force = force
    self.skip_partial = skip_partial
    self.page_completeness = page_completeness
    self.document_completeness = document_completeness
  
  def get_font_path(self,font_name,font_style):
    # Map from observed font names to available fonts
    font_map = {
      'Times': 'Times New Roman',
      'Helvetica': 'Arial'
    }
    font_name = font_map[font_name] if font_name in font_map else font_name
    font_style = ' ' + font_style if font_style else ''
    font_path = os.path.join(self.font_dir,'%s%s.ttf' % (font_name,font_style))
    if not os.path.exists(font_path):
      font_path = os.path.join(self.font_dir,'%s%s.ttf' % ('Arial',font_style))
      if not os.path.exists(font_path):
        font_path = os.path.join(self.font_dir,'%s.ttf' % ('Arial',))
    return font_path
  
  def set_patient_completeness(self,completeness,patient_id,status,indexer_version,details):
    print('Patient %s completeness: %s (%s)' % (patient_id, status, details))
    try:
      cursor = completeness.cursor()
      cursor.execute("""INSERT INTO patients VALUES (%s,%s,%s,%s,%s) 
        ON DUPLICATE KEY UPDATE
        patient_id=VALUES(patient_id),
        status=VALUES(status),
        update_date=VALUES(update_date),
        indexer_version=VALUES(indexer_version),
        details=VALUES(details);""",(patient_id,status,datetime.datetime.now(),indexer_version,details))
      completeness.commit()
    except:
      print('Failed to write to completeness database: ' + str(sys.exc_info()))
  
  def should_update_patient(self,completeness,patient_id,indexer_version):
    if self.force:
      return True
    # NOTE: goal is to not even load up the encrypted database or query services if database exists
    # Should we then go ahead and confirm database exists?
    # Perhaps if attempting to access database and not there, update completeness to indicate failure
    update_patient = True
    cursor = completeness.cursor()
    cursor.execute('select status, update_date, indexer_version from patients where patient_id = "%s"' % (patient_id,))
    patient_completeness = cursor.fetchone()
    # If we're processing this patient currently, or have processed this patient successfully with the current indexer, don't update
    # TODO: Add control to either "force" update or attempt update based on time interval
    # Skip partial will assume a patient in the database is complete
    skip_statuses = ['success']
    if self.skip_partial:
      skip_statuses.append('error')
    if patient_completeness and (patient_completeness[0] in skip_statuses):
      update_patient = False
    print('Should update patient %s: %s' % (patient_id,update_patient))
    return update_patient
  
  def set_document_completeness(self,completeness,patient_id,document_id,status,indexer_type,indexer_version,details):
    if not self.document_completeness:
      return
    print('Document %s completeness: %s (%s)' % (document_id, status, details))
    # patient_id VARCHAR(255) NOT NULL,
    # document_id VARCHAR(255) NOT NULL,
    # status VARCHAR(255) NOT NULL,
    # update_date TIMESTAMP DEFAULT '1970-01-01 00:00:01',
    # indexer_type VARCHAR(255) NOT NULL,
    # indexer_version VARCHAR(255) NOT NULL,
    # details TEXT,
    # PRIMARY KEY (patient_id,document_id)
    cursor = completeness.cursor()
    cursor.execute("""INSERT INTO documents VALUES (%s,%s,%s,%s,%s,%s,%s) 
        ON DUPLICATE KEY UPDATE
        patient_id=VALUES(patient_id),
        document_id=VALUES(document_id),
        status=VALUES(status),
        update_date=VALUES(update_date),
        indexer_type=VALUES(indexer_type),
        indexer_version=VALUES(indexer_version),
        details=VALUES(details);""",(
      patient_id,
      document_id,
      status,
      datetime.datetime.now(),
      indexer_type,
      indexer_version,
      details))
    completeness.commit()
  
  def should_update_document(self,completeness,patient_id,document_id,indexer_version):
    if self.force:
      return True
    update_document = True
    cursor = completeness.cursor()
    cursor.execute('select status, update_date, indexer_version from documents where patient_id = "%s" and document_id = "%s"' % (patient_id,document_id))
    document_completeness = cursor.fetchone()
    # If we're processing this patient currently, or have processed this patient successfully with the current indexer, don't update
    # TODO: Add control to either "force" update or attempt update based on last update date of eocument
    if document_completeness and document_completeness[0] == 'success' and document_completeness[2] == indexer_version:
      update_document = False
    print('Should update patient %s, document %s: %s' % (patient_id,document_id,update_document))
    return update_document
  
  def set_page_completeness(self,completeness,patient_id,document_id,page_number,status,indexer_type,indexer_version,details):
    if not self.page_completeness:      
      return
    print('Document %s page %s completeness: %s (%s)' % (document_id, page_number, status, details))
    cursor = completeness.cursor()
    cursor.execute("""INSERT INTO pages VALUES (%s,%s,%s,%s,%s,%s,%s,%s) 
        ON DUPLICATE KEY UPDATE
        patient_id=VALUES(patient_id),
        document_id=VALUES(document_id),
        page_number=VALUES(page_number),
        status=VALUES(status),
        update_date=VALUES(update_date),
        indexer_type=VALUES(indexer_type),
        indexer_version=VALUES(indexer_version),
        details=VALUES(details);""",(
      patient_id,
      document_id,
      page_number,
      status,
      datetime.datetime.now(),
      indexer_type,
      indexer_version,
      details))
    completeness.commit()
  
  def should_update_page(self,completeness,patient_id,document_id,page_number,indexer_version):
    if self.force:
      return True
    update_page = True
    cursor = completeness.cursor()
    cursor.execute('select status, update_date, indexer_version from pages where patient_id = "%s" and document_id = "%s" and page_number="%s"' % (patient_id,document_id,page_number))
    page_completeness = cursor.fetchone()
    # If we're processing this patient currently, or have processed this patient successfully with the current indexer, don't update
    # TODO: Should we skip "error" pages if the indexer version hasn't changed?
    if page_completeness and page_completeness[0] == page_completeness[0] == 'success' and page_completeness[2] == indexer_version:
      update_page = False
    print('Should update patient %s, document %s, page %s: %s' % (patient_id,document_id,page_number,update_page))
    return update_page
  
  def get_patient_db(self,patient,encryption_key):
    # Load the encrypted document database with the configured key
    print('Trying to load DB for ' + patient)
    db_path = os.path.join(self.db_dir,'%s.db' % (patient,))
    db = sqlcipher.connect(db_path)
    db.execute('pragma key="%s"' % (encryption_key,))

    # Ensure correct key for existing database
    try:
      # TODO: In future, detect version and migrate if necessary
      db.execute('SELECT count(*) FROM sqlite_master;')
    except:
      # If this fails then delete and re-initialize
      print('Key was incorrect for db: ' + db_path)
      os.remove(db_path)
      db = sqlcipher.connect(db_path)
      db.execute('pragma key="%s"' % (encryption_key,))
    
    # Create schema if necessary
    db.execute('CREATE TABLE IF NOT EXISTS document_overlay (id text, page text, type text, item number, value text, left number, top number, width number, height number)')
    db.execute('CREATE INDEX IF NOT EXISTS document_id ON document_overlay(id)')
    db.execute('CREATE INDEX IF NOT EXISTS document_id_and_page ON document_overlay(id,page)')
    # Note one optimization is to set detail=none. This disables "phrase queries". Analysis needed to determine size impacts of this
    # If we were only using the db to find the "existence" of terms, then for text documents pulling the note  to do highlighting,
    # and for PDFs going to overlay query, this could work.
    # https://www.sqlite.org/fts5.html
    # db.execute('CREATE VIRTUAL TABLE IF NOT EXISTS document_text USING fts5(id, page, text, detail=none, tokenize = unicode61);')
    db.execute('CREATE VIRTUAL TABLE IF NOT EXISTS document_text USING fts5(id, page, text, tokenize = unicode61);')
    db.commit()
    print('Loaded DB for ' + patient)
    return db

  def index_patient(self,patient_id,completeness):
    success = False
    # Patient Indexer Version (should increment for every child indexer)
    indexer_version_patient = '1.0.0'
    if self.should_update_patient(completeness,patient_id,indexer_version_patient):
      try:
        # Update completeness database to indicate that patient is being processed
        self.set_patient_completeness(completeness,patient_id,'processing', indexer_version_patient,'')
        # Load the encrypted patient database
        patient_db = self.get_patient_db(patient_id, patient_id)
        # For each patient document
        summary_response = self.session.dataorchestrator.summary(patient_id,'document')
        if summary_response.status_code == 200:
          print('Got document summary for ' + patient_id)
          success = True
          pat_doc_summary = summary_response.json()
          for document in pat_doc_summary['documents']:
            document_id = document['internalUUID']
            print('Indexing document ' + document_id)
            mime_type = document['documentContents'][0]['mimeType'] if len(document['documentContents']) > 0 else ''
            document_success = False
            if mime_type == 'application/pdf':
              document_success = self.index_document_pdf(completeness,patient_id,document_id,patient_db)
            elif mime_type != 'text/xml':
              document_success = self.index_document_text(completeness,patient_id,document_id,patient_db)
            if not document_success:
              success = False
          status = 'success' if success else 'error'
          detail = '' if success else 'failed documents'
          self.set_patient_completeness(completeness,patient_id,status,indexer_version_patient,detail)
        else:
          error_msg = 'Failed to fetch document summary. Status code: ' + str(summary_response.status_code)
          self.set_patient_completeness(completeness,patient_id,'error',indexer_version_patient,error_msg)
      except (KeyboardInterrupt, SystemExit):
        print(sys.exc_info())
        sys.exit()
      except:
        self.set_patient_completeness(completeness,patient_id,'error',indexer_version_patient,traceback.format_exc())
    else:
      success = True
    if completeness:
      completeness.cursor().close()
    return success
  
  def index_document_text(self,completeness,patient_id,document_id,patient_db):
    success = False
    # Document Indexer Version (should increment for every child indexer)
    indexer_version_document_text = '1.0.0'
    if self.should_update_document(completeness,patient_id,document_id,indexer_version_document_text):  
      # Update completeness database to indicate that document is being processed
      self.set_document_completeness(completeness,patient_id,document_id,'processing','text',indexer_version_document_text,'')
      try:
        simple_content_response = self.session.dataorchestrator.simple_content(document_id)
        if simple_content_response.status_code == 200:
          simple_content = simple_content_response.text
          cleaned_text_regex = re.compile('<.*?>')
          cleaned_text = re.sub(cleaned_text_regex, '', simple_content)
          patient_db.execute('INSERT INTO document_text VALUES (?, ?, ?)', (document_id, 1, cleaned_text))
          patient_db.commit()
          self.set_document_completeness(completeness,patient_id,document_id,'success','text',indexer_version_document_text,'')
          success = True
        else:
          error_msg = 'Failed to fetch simple content. Status code: ' + str(simple_content_response.status_code)
          self.set_document_completeness(completeness,patient_id,document_id,'error','text',indexer_version_document_text,error_msg)
      except (KeyboardInterrupt, SystemExit):
        sys.exit()
      except:
        self.set_document_completeness(completeness,patient_id,document_id,'error','text',indexer_version_document_text,traceback.format_exc())
    else:
      success = True
    return success

  
  def index_document_pdf(self,completeness,patient_id,document_id,patient_db):
    success = False
    # Document Indexer Version (should increment for every child indexer)
    indexer_version_document_pdf = '1.0.0'
    if self.should_update_document(completeness,patient_id,document_id,indexer_version_document_pdf):
      # Update completeness database to indicate that document is being processed
      self.set_document_completeness(completeness,patient_id,document_id,'processing','pdf',indexer_version_document_pdf,'')
      try:
        response = self.session.dataorchestrator.patient_object_by_doc(document_id)
        if response.status_code == 200:
          success = True
          stringXml = response.json()['documents'][0]['stringContent']
          if stringXml.startswith('<!DOCTYPE'):
            stringXml = self.dtd + stringXml[stringXml.index('>')+1:]
          root = ET.fromstring(stringXml)
          detail = ''
          for page in root.findall('pages/page'):
            page_number = page.find('pageNumber').text
            extracted_text = page.find('extractedText/content').text
            plain_text = page.find('plainText').text
            page_success = False
            if extracted_text:
              page_success = self.index_document_page_hocr(completeness,patient_id,document_id,page_number,extracted_text,patient_db)
            elif plain_text:
              page_success = self.index_document_page_text(completeness,patient_id,document_id,page_number,plain_text,patient_db)
            else:
              # We will consider this success for now since there isn't anything we can do
              print('no plain_text or extracted_text for page ' + page_number + ' - ' + str(ET.tostring(page)))
              page_success = True
            if not page_success:
              success = False          
          status = 'success' if success else 'error'
          detail = '' if success else 'page failures'
          self.set_document_completeness(completeness,patient_id,document_id,status,'pdf',indexer_version_document_pdf,detail)
        else:
          error_msg = 'Failed to fetch document. Status code: ' + response.status_code
          self.set_document_completeness(completeness,patient_id,document_id,'error','pdf',indexer_version_document_pdf,error_msg)
      except (KeyboardInterrupt, SystemExit):
        sys.exit()
      except:
        self.set_document_completeness(completeness,patient_id,document_id,'error','pdf',indexer_version_document_pdf,traceback.format_exc())
    else:
      success = True
    return success
  
  def index_document_page_hocr(self,completeness,patient_id,document_id,page_number,extracted_text,patient_db):
    success = False
    # Page Indexer Version
    indexer_version_page_hocr = '1.0.0'
    if self.should_update_page(completeness,patient_id,document_id,page_number,indexer_version_page_hocr):
      # Update completeness database to indicate that document is being processed
      self.set_page_completeness(completeness,patient_id,document_id,page_number,'processing','hocr',indexer_version_page_hocr,'')
      try:
        bad_tags = [
          '<meta http-equiv="Content-Type" content="text/html;charset=utf-8">',
          '<meta name="ocr-system" content="tesseract">'
        ]
        cleanedHtml = extracted_text
        for bad_tag in bad_tags:
          cleanedHtml = cleanedHtml.replace(bad_tag,'')
        cleanedHtml = cleanedHtml.replace('&?','') # Not sure if this is right but works for this doc
        # TODO: This breaks on html entitites not defined in DTD. Not sure what to do yet as the html
        # already has a DTD:
        # <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
        # need to add everything including eacute é
        if cleanedHtml.startswith('<!DOCTYPE'):
          cleanedHtml = self.dtd + cleanedHtml[cleanedHtml.index('>')+1:] 
        pageRoot = ET.fromstring(cleanedHtml)
        if pageRoot.tag == 'div' and pageRoot.attrib['class'] == 'ocr_page':
          pageDetails = pageRoot.attrib['title'].split(';')
        else:
          pageDetails = pageRoot.find('.//div[@class="ocr_page"]').get('title').split(';')
        pageHeight = 0
        pageWidth = 0
        for pageDetail in pageDetails:
          if pageDetail.strip().startswith('bbox'):
            coordinates = [pageDetail.replace(';','') for pageDetail in pageDetail.strip().split(" ")[1:]]
            pageWidth = int(coordinates[2])
            pageHeight = int(coordinates[3])
        ocrWords = pageRoot.findall('.//span[@class="ocrx_word"]')
        overlays = []
        for ocrWord in ocrWords:
          text = ocrWord.text if ocrWord.text else ''.join(ocrWord.itertext())
          coordinates = ocrWord.get('title')
          coordinates = [int(coordinate.replace(';','')) for coordinate in coordinates.split(" ")[1:5]] 
          if text:
            overlays.append({
              'text': text,
              'left': int(coordinates[0]) / pageWidth,
              'top': int(coordinates[1]) / pageHeight,
              'width': (int(coordinates[2]) - int(coordinates[0])) / pageWidth,
              'height': (int(coordinates[3]) - int(coordinates[1])) / pageHeight
            })
        self.insert_page_data(patient_db, document_id, page_number, overlays)
        self.set_page_completeness(completeness,patient_id,document_id,page_number,'success','hocr',indexer_version_page_hocr,'')
        success = True
      except (KeyboardInterrupt, SystemExit):
        sys.exit()
      except:
        error_msg = 'Error processing extracted text: ' + traceback.format_exc()
        self.set_page_completeness(completeness,patient_id,document_id,page_number,'error','hocr',indexer_version_page_hocr, error_msg)
    else:
      success = True
    return success
  
  def index_document_page_text(self,completeness,patient_id,document_id,page_number,plain_text,patient_db):
    success = False
    # Page Indexer Version
    indexer_version_page_text = '1.0.0'
    if self.should_update_page(completeness,patient_id,document_id,page_number,indexer_version_page_text):
      # Update completeness database to indicate that document is being processed
      self.set_page_completeness(completeness,patient_id,document_id,page_number,'processing','text',indexer_version_page_text,'')
      try:
        plainTextRoot = ET.fromstring('%s<plainText>%s</plainText>' % (self.dtd,plain_text))
        pageStyleTag = plainTextRoot.find('.//div[@class="page"]')
        pageStyle = pageStyleTag.attrib['style']
        pageDetails = {s.split(':')[0]:s.split(':')[1] for s in pageStyle.split(';') if len(s.split(':')) > 1}
        pageHeight = float(pageDetails['height'].replace('pt',''))
        pageWidth = float(pageDetails['width'].replace('pt',''))
        plainTextWords = plainTextRoot.findall('.//div[@class="p"]')
        overlays = []
        for plainTextWord in plainTextWords:
          text = plainTextWord.text.strip()
          if text:
            wordStyle = plainTextWord.attrib['style']
            wordDetails = {s.split(':')[0]:s.split(':')[1] for s in wordStyle.split(';') if s != ''}
            fontSize = int(float(wordDetails['font-size'].replace('pt','')))
            fontWeight = wordDetails['font-weight'] if 'font-weight' in wordDetails else ''
            fontFamily = wordDetails['font-family'] if 'font-family' in wordDetails else 'Helvetica'
            fontPath = self.get_font_path(fontFamily,fontWeight)
            font = ImageFont.truetype(fontPath,fontSize)
            # TODO: This will be inaccurate for some types of extractions
            wordDimensions = font.getsize(text)
            overlays.append({
              'text': text,
              'left': float(wordDetails['left'].replace('pt','')) / pageWidth,
              'top': float(wordDetails['top'].replace('pt','')) / pageHeight,
              'width': wordDimensions[0] / pageWidth,
              'height': wordDimensions[1] / pageWidth
            })
        self.insert_page_data(patient_db, document_id, page_number, overlays)
        self.set_page_completeness(completeness,patient_id,document_id,page_number,'success','text',indexer_version_page_text,'')
        success = True
      except (KeyboardInterrupt, SystemExit):
        sys.exit()
      except:
        error_msg = 'Error processing plain text: ' + traceback.format_exc()
        self.set_page_completeness(completeness,patient_id,document_id,page_number,'error','text',indexer_version_page_text, error_msg)
    else:
      success = True
    return success

  def insert_page_data(self, patient_db, document_id, page_number, overlays):
    page_text_items = []
    for overlay in overlays:
      page_text_items.append(overlay['text'])
      patient_db.execute('INSERT INTO document_overlay VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)', (
        document_id,
        page_number,
        'hocr',
        len(page_text_items),
        overlay['text'],
        overlay['left'],
        overlay['top'],
        overlay['width'],
        overlay['height']))
    page_text = ' '.join(page_text_items)
    patient_db.execute('INSERT INTO document_text VALUES (?, ?, ?)', (document_id, page_number, page_text))
    patient_db.commit()

# ex: python __init__.py -dbh 10.1.16.132 -dbu completeness -p 90d41a62-0f3c-4c61-8e3b-d44537a9d625 -u alarocca@apixio.com -d ../document-search-service/db/ -f /System/Library/Fonts/Supplemental/
if __name__ == "__main__":
  parser = ArgumentParser()
  parser.add_argument("-p", "--patient",
                      help="Patient for whom to build index.")
  parser.add_argument("-l", "--list",
                      help="Patient list for whom to build index.")
  parser.add_argument("-u", "--user", required=True,
                      help="Username for accessing Apixio services.")
  parser.add_argument("-e", "--environment", default="development",
                      help="The environment for patient data (development or production).")
  parser.add_argument("-f", "--fonts", default="./resources/fonts",
                      help="The location of the font directory.")
  parser.add_argument("-d", "--directory", default="/patient-index/",
                      help="The location of the db directory.")
  parser.add_argument("-t", "--threads", default="8",
                      help="Number of threads to use for Thread Pool. Default is 8.")
  parser.add_argument("-dbh", "--dbhost",
                      help="Host for the completeness database.")
  parser.add_argument("-dbu", "--dbuser",
                      help="User for the completeness database.")
  parser.add_argument("-F", "--force", dest='force', action='store_true',
                      help="If specified, completeness will not be checked before indexing.")
  parser.add_argument("-S", "--skip-partial", dest='skip', action='store_true',
                      help="If specified, patients with any record in completeness will be skipped.")
  parser.set_defaults(force=False)
  parser.add_argument("-npc", "--no-page-completeness", dest='page_completeness', action='store_false',
                      help="If specified, completeness will not be managed at page level.")
  parser.set_defaults(page_completeness=True)
  parser.add_argument("-ndc", "--no-document-completeness", dest='document_completeness', action='store_false',
                      help="If specified, completeness will not be managed at document level.")
  parser.set_defaults(document_completeness=True)
  args = parser.parse_args()
  
  force = args.force
  if force:
    print('Force mode: completeness will not be checked before indexing.')
  skip_partial = args.skip
  if skip_partial:
    print('Skip mode: Patients with any record in completeness will be skipped.')
  page_completeness = args.page_completeness
  if not page_completeness:
    print('Page completeness disabled.')
  document_completeness = args.document_completeness
  if not document_completeness:
    print('Document completeness disabled.')
  num_threads = int(args.threads)

  session = apxapi.APXSession(args.user, environment=args.environment)
  indexer = PatientSearchIndexer(session,
                                 args.fonts,
                                 args.directory,
                                 force,
                                 skip_partial,
                                 page_completeness,
                                 document_completeness)
  # if not force:
  dbpassword = getpass.getpass('Password for %s on %s: ' % (args.dbuser, args.dbhost))
  completeness = connector.connect(user=args.dbuser, password=dbpassword, host=args.dbhost, database='completeness')
  if args.patient:
      indexer.index_patient(args.patient, completeness)
  elif args.list:
      print('Indexing patient with ' + str(num_threads) + ' threads.')
      #tp = ThreadPool(indexer.index_patient, retries=0, num_threads=num_threads)
      #_ = [tp.add_task((line.strip(), completeness)) for line in open(args.list).readlines()]      
      for line in open(args.list).readlines():
        indexer.index_patient(line.strip(), completeness)
      #results = tp.wait_completion()
      #print('Failed to process %d patients' % (len([x for x in results.values() if x is None]),))
