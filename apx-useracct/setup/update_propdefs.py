import argparse
import apxapi
import json
import sys

from collections import defaultdict

def main():
    args = sys.argv[1:]
    parsed_args = get_parser().parse_args(args)

    proposed_propdefs = json.load(parsed_args.propdefs_json_file,
                                  object_pairs_hook=enforce_no_duplicate_keys)

    s = get_apxsession(parsed_args)

    current_propdefs = {bag: s.useraccounts.get_project_propdefs(bag)
                        for bag in proposed_propdefs.keys()}

    for (bag, propdefs_resp) in current_propdefs.items():
        if propdefs_resp.status_code != 200:
            raise IOError(f'Could not fetch propdefs for bag {bag}. Received HTTP status code {propdefs_resp.status_code}.')

    current_propdefs = {bag: resp.json()
                        for (bag, resp) in current_propdefs.items()}

    propdefs_to_add = get_propdefs_to_add(proposed_propdefs, current_propdefs)

    if len(propdefs_to_add) == 0:
        print('No propdefs found to add.')
    else:
        print('Found propdefs to add: ' + str(propdefs_to_add))

        if parsed_args.execute:
            print(f'Adding the above propdefs to {parsed_args.environment}.')
            add_propdefs(propdefs_to_add, s)
        else:
            print(f'Running in preview mode. Re-run this tool with `--execute` to commit to adding the above propdefs to {parsed_args.environment}.')

def get_parser():
    parser = argparse.ArgumentParser(description='Update Project propdefs')
    parser.add_argument('propdefs_json_file',
                        help='path to "propdefs.json"',
                        nargs='?',
                        type=argparse.FileType('r'),
                        default='propdefs.json')
    parser.add_argument('--execute',
                        help='Add eligible propdefs. Operates in "dry-run" mode otherwise.',
                        action='store_true')
    parser.add_argument('--production',
                        help='Create a project in Production',
                        action='store_const',
                        dest='environment',
                        default=apxapi.DEV,
                        const=apxapi.PRD)

    parser.add_argument('--use-apxapi-dotfile',
                        action='store_true')

    return parser

def enforce_no_duplicate_keys(pairs):
    counter = defaultdict(int)
    for (k, v) in pairs:
        counter[k] += 1

    dup_keys = {k: v for (k, v) in counter.items() if v > 1}
    if len(dup_keys) > 0:
        raise KeyError('Duplicate keys found: ' + str(dup_keys))

    return {k: v for (k, v) in pairs}

def get_apxsession(args):
    if args.use_apxapi_dotfile:
        s = apxapi.session(environment=args.environment)
    else:
        username = input("Enter your Apixio email: ")
        s = apxapi.APXSession(username, environment=args.environment)

    return s

def get_propdefs_to_add(proposed_propdefs, current_propdefs):
    mismatched_propdef_types = {}
    missing_propdefs = {}

    for bag in proposed_propdefs.keys():
        missing_bag_propdefs = {k: v
                                for (k, v) in proposed_propdefs[bag].items()
                                if current_propdefs[bag].get(k) == None}

        if len(missing_bag_propdefs) > 0:
            missing_propdefs[bag] = missing_bag_propdefs

        mismatch_propdefs = {k: v
                             for (k, v) in proposed_propdefs[bag].items()
                             if current_propdefs[bag].get(k) != None
                             and current_propdefs[bag].get(k) != v}

        if len(mismatch_propdefs) > 0:
            mismatched_propdef_types[bag] = mismatch_propdefs

    if len(mismatched_propdef_types) > 0:
        raise KeyError('Existing keys already exist with different types: ' + str(mismatched_propdef_types))

    return missing_propdefs

def add_propdefs(propdefs_to_add, s):
    for bag in propdefs_to_add:
            for (propdef_name, propdef_type) in propdefs_to_add[bag].items():
                print(f'''Running s.useraccounts.post_project_propdef('{propdef_name}', '{propdef_type}', '{bag}')''')
                resp = s.useraccounts.post_project_propdef(propdef_name, propdef_type, bag)
                assert(resp.status_code == 200)


if __name__ == '__main__':
    main()
