pip3 install -r requirements.txt
pyinstaller --onefile --distpath . -y --collect-all apxprotobufs sdkannotest.py
rm -rf build
rm *.spec