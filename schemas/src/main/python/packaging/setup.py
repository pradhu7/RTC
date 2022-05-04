import setuptools
from distutils.core import setup

setup(
    name='apx-schemas',
    version='${python_version}',
    author='Kyle McGovern',
    author_email='kmcgovern@apixio.com',
    url='https://github.com/Apixio/mono/tree/dev/schemas',
    packages=setuptools.find_packages(),
    install_requires=['protobuf==${com.google.protobuf-version}'],
    include_package_data=True
)
