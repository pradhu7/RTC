import sys
from distutils.core import setup

requirements = open('requirements.txt', 'r').read().strip().split()
requirements_test = open('requirements-test.txt', 'r').read().strip().split()
version = open('version.txt', 'r').read().strip()

setup(
    name='apxprotobufs',
    version=version,
    author='Tim Wang',
    author_email='twang@apixio.com',
    url='https://github.com/Apixio/apx-schemas',
    packages=['apxprotobufs'],
    install_requires=requirements,
    tests_require=requirements_test,
    include_package_data=True
)
