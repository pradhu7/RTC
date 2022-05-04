"""
Apixio SDK for Python
An adaptor that allows python functions, classes and applications to be packaged as F(x)s for the MC catalog
"""
import os
import subprocess
import sys
from datetime import datetime

from setuptools import setup, find_packages


def read_requirements(req_file):
    """
    Reads and parse a requirements text file (from a path),
    to a list of versioned package dependencies.

    NOTE: Any dependency without the version explicetly specified
    with the "==" operator will be ignored by apxsdk.

    We are doing this to ensure that our builds won't break because
    of transitive dependencies without proper version lock.

    Check our "requirements.in" directory for more info.
    """
    with open(req_file, 'r') as requirements_file:
        requirements_lines = requirements_file.read().strip().split()

    requirements = [line for line in requirements_lines if ('#' not in line) & ('==' in line)]

    return requirements


VERSION = {
    "major": 0,
    "minor": 1,
    "patch": 0
}

VERSION_FILENAME = 'apxsdk/__version__.py'

# If we are installing from a package, use the version file that already exists
if 'install' in sys.argv and os.path.exists(VERSION_FILENAME):
    with open(VERSION_FILENAME, 'r') as vf:
        VERSION_DATA = vf.readlines()
        VERSION_STRING = VERSION_DATA[1].split("VERSION = ")[1].replace("'", '').strip()
else:
    # We are building or installing from a clean checkout without a version file
    try:
        if 'develop' in sys.argv[1] or 'dist' in sys.argv[1] or 'install' in sys.argv:
            try:
                # Jenkins git is detached from head so the branch can't be easily
                # retrieved via git cli
                CURRENT_BRANCH = os.environ["GIT_BRANCH"]
            except (KeyError, ValueError):
                CMD_OUTPUT = subprocess.check_output(['git', 'symbolic-ref', 'HEAD']) \
                    .decode("utf-8")
                CURRENT_BRANCH = CMD_OUTPUT.split('/')[2].rstrip()
            if not CURRENT_BRANCH.endswith('master'):
                # rev should conform to PEP-0440
                # https://www.python.org/dev/peps/pep-0440/#pre-releases
                patch = "dev%s" % datetime.today().strftime('%Y%m%d%H%M%S')
                VERSION["patch"] = patch  # type: ignore
    except subprocess.CalledProcessError:
        pass
    VERSION_STRING = "{}.{}.{}".format(VERSION["major"], VERSION["minor"], VERSION["patch"])
    with open('apxsdk/__version__.py', 'w') as VER:
        VER.write("# pylint: skip-file\n")
        VER.write("VERSION = '{}'\n".format(VERSION_STRING))


REQUIREMENTS = read_requirements('requirements.txt')
REQUIREMENTS_TEST = read_requirements('requirements.txt')

CONFIG = {
    'description': "Library and commands for Apixio's Python SDK",
    'url': 'https://github.com/Apixio/apx-sdk/sdkcore/',
    'version': VERSION_STRING,
    'packages': find_packages(),
    'name': 'python-apxsdk',
    'install_requires': REQUIREMENTS,
    'setup_requires': ['pytest-runner', 'pytest-pylint'],
    'tests_require': REQUIREMENTS_TEST,
    'entry_points': {
        'console_scripts': ['aolfs=apxsdk.sdk:main']
    },
    'include_package_data': True
}

setup(**CONFIG)
