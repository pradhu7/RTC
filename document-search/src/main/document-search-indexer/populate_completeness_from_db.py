from pysqlcipher3 import dbapi2 as sqlcipher
from mysql import connector
import getpass
from argparse import ArgumentParser
import os
import datetime
import traceback
import logging
import sys


def populate_completeness(directory,patient,completeness):
  cursor = completeness.cursor()
  # First check the completeness database. If patient is already in there, continue
  cursor.execute('select status, update_date, indexer_version from patients where patient_id = "%s"' % (patient,))
  patient_completeness = cursor.fetchone()
  if patient_completeness:
    logging.info('Patient %s already in completeness: %s' % (patient,patient_completeness[0]))
    return
  
  # If not in completeness, try to load the encrypted patient database
  logging.info('Trying to load DB for ' + patient)
  db_path = os.path.join(directory,'%s.db' % (patient,))
  if not os.path.exists(db_path):
    logging.info('DB does not exist: ' + db_path)
    return
  
  # Try to conntect to the database with a key
  db = sqlcipher.connect(db_path)
  db.execute('pragma key="%s"' % (patient,))

  # Ensure correct key for existing database
  try:
    db.execute('SELECT count(*) FROM sqlite_master;')
    print('Loaded existing patient database.')
  except:
    # If we have the incorrect key, there is nothing we can do.
    print('Incorrect key : ' + db_path)
    return
  
  # Get distinct ids For every document in document_text (select distinct id) assume success
  # We will query for pages next and update this table to error (assuming page failures) for all document ids found there
  logging.info('Adding patient documents from document_text to database with status success')
  for document in db.execute('select distinct id from document_text').fetchall():
    # Since some of these may actually have failed, should we put completeness in as Error so they will try again?
    # Before doing that, we need to ensure uniqueness so we don't duplicate data in the database
    # If these are text documents, success can probaby be assumed, so maybe we add a mode to the indexer to try to update pdf documents?
    cursor.execute("""INSERT INTO documents VALUES (%s,%s,%s,%s,%s,%s,%s) 
        ON DUPLICATE KEY UPDATE
        patient_id=VALUES(patient_id),
        document_id=VALUES(document_id),
        status=VALUES(status),
        update_date=VALUES(update_date),
        indexer_type=VALUES(indexer_type),
        indexer_version=VALUES(indexer_version),
        details=VALUES(details);""",(
      patient,
      document[0],
      'success',
      datetime.datetime.now(),
      'text',
      '1.0.0',
      ''))
  
  # Get all the pages. These are only for PDF documents, so assume success for every page, but error for every document so we will try again later
  logging.info('Adding patient pages from document_overlay to database with status success and documents with status error')
  for page in db.execute('select distinct id, page from document_overlay'):
    # For every page in document overlay (select distinct id, page) assume success as it wouldn't be here otherwise
    cursor.execute("""INSERT INTO documents VALUES (%s,%s,%s,%s,%s,%s,%s) 
        ON DUPLICATE KEY UPDATE
        patient_id=VALUES(patient_id),
        document_id=VALUES(document_id),
        status=VALUES(status),
        update_date=VALUES(update_date),
        indexer_type=VALUES(indexer_type),
        indexer_version=VALUES(indexer_version),
        details=VALUES(details);""",(
      patient,
      page[0],
      'error',
      datetime.datetime.now(),
      'text',
      '1.0.0',
      'inferred-from-db'))
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
      patient,
      page[0],
      page[1],
      'success',
      datetime.datetime.now(),
      'hocr',
      '1.0.0',
      ''))

  # Since we got the patient database, lets put a row for patient completeness, but do it last in case this program crashes ;)
  logging.info('Adding patient to database with status error (inferred-from-db)')
  cursor.execute("""INSERT INTO patients VALUES (%s,%s,%s,%s,%s) 
      ON DUPLICATE KEY UPDATE
      patient_id=VALUES(patient_id),
      status=VALUES(status),
      update_date=VALUES(update_date),
      indexer_version=VALUES(indexer_version),
      details=VALUES(details);""",(
    patient,
    'error',
    datetime.datetime.now(),
    '1.0.1',
    'inferred-from-db')
  )
    
  completeness.commit()
  

# TODO: before running this, migrate the historical completeness database to MySQL to retain valid completeness data
# ex: python __init__.py -dbh 10.1.16.132 -dbu completeness -l list.txt -d ../document-search-service/db/
if __name__ == "__main__":
  parser = ArgumentParser()
  parser.add_argument("-l", "--list",
                      help="Patient list for whom to build index.")
  parser.add_argument("-d", "--directory", default="/patient-index/",
                      help="The location of the db directory.")
  parser.add_argument("-dbh", "--dbhost",
                      help="Host for the completeness database.")
  parser.add_argument("-dbu", "--dbuser",
                      help="User for the completeness database.")
  args = parser.parse_args()

  dbpassword = getpass.getpass('Password for %s on %s: ' % (args.dbuser, args.dbhost))
  completeness = connector.connect(user=args.dbuser, password=dbpassword, host=args.dbhost, database='completeness')
  logging.basicConfig(
    filename='/mnt/data/backfill_completeness.log',
    format='%(asctime)s %(levelname)-8s %(message)s',
    level=logging.INFO,
    datefmt='%Y-%m-%d %H:%M:%S')
  # For each patient id
  for line in open(args.list).readlines():
    patient = line.strip()
    try:
      populate_completeness(args.directory, patient, completeness)
    except (KeyboardInterrupt, SystemExit):
      print(sys.exc_info())
      sys.exit()
    except:
        logging.error('Exception populating patient index: ' + traceback.format_exc())
