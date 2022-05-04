
Start a terminal at the directory that holds this README file.

To compile fx's into single file executables, you need a python version that supports pyinstaller. 
```
env PYTHON_CONFIGURE_OPTS="--enable-shared" pyenv install 3.6.8 --patch < <(curl -sSL https://github.com/python/cpython/commit/8ea6353.patch)
```
(or, if 3.6.8 already exists, you can use 3.6.9 for dev)

Then, setup pyenv:
```
pyenv virtualenv 3.6.9 pythonsdk
pydev local pythonsdk
```

Then install:
```
pip install --upgrade pip
pip install -r requirements.txt 
python setup.py install

```