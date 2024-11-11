#!/usr/bin/env python3
"""
# Translation Management Script

This script is designed to manage translations for a project by performing two operations:
1) Getting the English translations from all modules.
2) Splitting translations into separate files for each module and language into a single file.

More detailed specifications are described in the docs/0002-atlas-translations-management.rst design doc.
"""
import argparse
import os
import re
import sys
from lxml import etree


def parse_arguments():
    """
    This function is the argument parser for this script.
    The script takes only one of the two arguments --split or --combine.
    Additionally, the --replace-underscore argument can only be used with --split.
    """
    parser = argparse.ArgumentParser(description='Split or Combine translations.')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--split', action='store_true',
                       help='Split translations into separate files for each module and language.')
    group.add_argument('--combine', action='store_true',
                       help='Combine the English translations from all modules into a single file.')
    parser.add_argument('--replace-underscore', action='store_true',
                        help='Replace underscores with "-r" in language directories (only with --split).')
    return parser.parse_args()


def append_element_and_comment(element, previous_element, root):
    """
    Appends the given element to the root XML element, preserving the previous element's comment if exists.

    Args:
        element (etree.Element): The XML element to append.
        previous_element (etree.Element or None): The previous XML element before the current one.
        root (etree.Element): The root XML element to append the new element to.

    Returns:
        None
    """
    try:
        # If there was a comment before the current element, add it first.
        if isinstance(previous_element, etree._Comment):
            previous_element.tail = '\n\t'
            root.append(previous_element)

        # Indent all elements with one tab.
        element.tail = '\n\t'
        root.append(element)

    except Exception as e:
        print(f"Error appending element and comment: {e}", file=sys.stderr)
        raise


def get_translation_file_path(modules_dir, module_name, lang_dir, create_dirs=False):
    """
    Retrieves the path of the translation file for a specified module and language directory.

    Parameters:
        modules_dir (str): The path to the base directory containing all the modules.
        module_name (str): The name of the module for which the translation path is being retrieved.
        lang_dir (str): The name of the language directory within the module's directory.
        create_dirs (bool): If True, creates the parent directories if they do not exist. Defaults to False.

    Returns:
        str: The path to the module's translation file (Localizable.strings).
    """
    try:
        lang_dir_path = os.path.join(modules_dir, module_name, 'src', 'main', 'res', lang_dir, 'strings.xml')
        if create_dirs:
            os.makedirs(os.path.dirname(lang_dir_path), exist_ok=True)
        return lang_dir_path
    except Exception as e:
        print(f"Error creating directory path: {e}", file=sys.stderr)
        raise


def write_translation_file(modules_dir, root, module, lang_dir):
    """
    Writes the XML root element to a strings.xml file in the specified language directory.

    Args:
        modules_dir (str): The root directory of the project.
        root (etree.Element): The root XML element to be written.
        module (str): The name of the module.
        lang_dir (str): The language directory to write the XML file to.

    Returns:
        None
    """
    try:
        translation_file_path = get_translation_file_path(modules_dir, module, lang_dir, create_dirs=True)
        tree = etree.ElementTree(root)
        tree.write(translation_file_path, encoding='utf-8', xml_declaration=True)
    except Exception as e:
        print(f"Error writing translations to file.\n Module: {module}\n Error: {e}", file=sys.stderr)
        raise


def get_modules_to_translate(modules_dir):
    """
    Retrieve the names of modules that have translation files for a specified language.

    Parameters:
        modules_dir (str): The path to the directory containing all the modules.

    Returns:
        list of str: A list of module names that have translation files for the specified language.
    """
    try:
        modules_list = [
            directory for directory in os.listdir(modules_dir)
            if (
                os.path.isdir(os.path.join(modules_dir, directory))
                and os.path.isfile(get_translation_file_path(modules_dir, directory, 'values'))
                and directory != 'i18n'
            )
        ]
        return modules_list
    except FileNotFoundError as e:
        print(f"Directory not found: {e}", file=sys.stderr)
        raise
    except PermissionError as e:
        print(f"Permission denied: {e}", file=sys.stderr)
        raise


def process_module_translations(module_root, combined_root, module):
    """
    Process translations from a module and append them to the combined translations.

    Parameters:
        module_root (etree.Element): The root element of the module's translations.
        combined_root (etree.Element): The combined translations root element.
        module (str): The name of the module.

    Returns:
        etree.Element: The updated combined translations root element.
    """
    previous_element = None
    for idx, element in enumerate(module_root.getchildren(), start=1):
        try:
            try:
                translatable = element.attrib.get('translatable', True)
            except KeyError as e:
                print(f"Error processing element #{idx} from module {module}: "
                      f"Missing key 'translatable' in element attributes: {e}", file=sys.stderr)
                raise
            except Exception as e:
                print(f"Error processing element #{idx} from module {module}: "
                      f"Unexpected error accessing 'translatable' attribute: {e}", file=sys.stderr)
                raise

            if (
                    translatable and translatable != 'false'  # Check for the translatable property.
                    and element.tag in ['string', 'string-array', 'plurals']  # Only those types are read by transifex.
                    and (not element.nsmap
                         or element.nsmap and not element.attrib.get('{%s}ignore' % element.nsmap["tools"]))
            ):
                try:
                    element.attrib['name'] = '.'.join([module, element.attrib.get('name')])
                except KeyError as e:
                    print(f"Error setting attribute 'name' for element #{idx} from module {module}: Missing key 'name':"
                          f" {e}", file=sys.stderr)
                    raise
                except Exception as e:
                    print(f"Error setting attribute 'name' for element #{idx} from module {module}: Unexpected error:"
                          f" {e}", file=sys.stderr)
                    raise

                try:
                    append_element_and_comment(element, previous_element, combined_root)
                except Exception as e:
                    print(f"Error appending element #{idx} and comment from module {module}: {e}", file=sys.stderr)
                    raise

            # To check for comments in the next round.
            previous_element = element

        except Exception as e:
            print(f"Error processing element #{idx} from module {module}: {e}", file=sys.stderr)
            raise

    return combined_root


def combine_translations(modules_dir):
    """
    Combine translations from all specified modules into a single XML element.

    Parameters:
        modules_dir (str): The directory containing the modules.

    Returns:
        etree.Element: An XML element representing the combined translations.
    """
    try:
        combined_root = etree.Element('resources')
        combined_root.text = '\n\t'

        modules = get_modules_to_translate(modules_dir)
        for module in modules:
            try:
                translation_file = get_translation_file_path(modules_dir, module, 'values')
                module_translations_tree = etree.parse(translation_file)
                module_root = module_translations_tree.getroot()
                combined_root = process_module_translations(module_root, combined_root, module)

                # Put a new line after each module translations.
                if len(combined_root):
                    combined_root[-1].tail = '\n\n\t'

            except etree.XMLSyntaxError as e:
                print(f"Error parsing XML file {translation_file}: {e}", file=sys.stderr)
                raise
            except FileNotFoundError as e:
                print(f"Translation file not found: {e}", file=sys.stderr)
                raise
            except Exception as e:
                print(f"Error processing module '{module}': {e}", file=sys.stderr)
                raise

        # Unindent the resources closing tag.
        if len(combined_root):
            combined_root[-1].tail = '\n'
        return combined_root

    except Exception as e:
        print(f"Error combining translations: {e}", file=sys.stderr)
        raise


def combine_translation_files(modules_dir=None):
    """
    Combine translation files from different modules into a single file.
    """
    try:
        if not modules_dir:
            modules_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        combined_root_element = combine_translations(modules_dir)
        write_translation_file(modules_dir, combined_root_element, 'i18n', 'values')
    except Exception as e:
        print(f"Error combining translation files: {e}", file=sys.stderr)
        raise


def get_languages_dirs(modules_dir):
    """
    Retrieve directories containing language files for translation.

    Args:
        modules_dir (str): The directory containing all the modules.

    Returns:
        list: A list of directories containing language files for translation. Each directory represents
              a specific language and starts with the 'values-' extension.

    Example:
        Input:
            get_languages_dirs('/path/to/modules')
        Output:
            ['values-ar', 'values-uk', ...]
    """
    try:
        lang_parent_dir = os.path.join(modules_dir, 'i18n', 'src', 'main', 'res')
        languages_dirs = [
            directory for directory in os.listdir(lang_parent_dir)
            if (
                    directory.startswith('values-')
                    and 'strings.xml' in os.listdir(os.path.join(lang_parent_dir, directory))
            )
        ]
        return languages_dirs
    except FileNotFoundError as e:
        print(f"Directory not found: {e}", file=sys.stderr)
        raise
    except PermissionError as e:
        print(f"Permission denied: {e}", file=sys.stderr)
        raise


def separate_translation_to_modules(modules_dir, lang_dir):
    """
    Separates translations from a translation file into modules.

    Args:
        modules_dir (str): The directory containing all the modules.
        lang_dir (str): The directory containing the translation file being split.

    Returns:
        dict: A dictionary containing the translations separated by module.
        {
            'module_1_name': etree.Element('resources')_1.
            'module_2_name': etree.Element('resources')_2.
            ...
        }
    """
    translations_roots = {}
    try:
        # Parse the translation file
        file_path = get_translation_file_path(modules_dir, 'i18n', lang_dir)
        module_translations_tree = etree.parse(file_path)
        root = module_translations_tree.getroot()
        previous_entry = None

        # Iterate through translation entries, with index starting from 1 for readablity
        for i, translation_entry in enumerate(root.getchildren(), start=1):
            try:
                if not isinstance(translation_entry, etree._Comment):
                    # Split the key to extract the module name
                    module_name, key_remainder = translation_entry.attrib['name'].split('.', maxsplit=1)
                    translation_entry.attrib['name'] = key_remainder

                    # Create a dictionary entry for the module if it doesn't exist
                    if module_name not in translations_roots:
                        translations_roots[module_name] = etree.Element('resources')
                        translations_roots[module_name].text = '\n\t'

                    # Append the translation entry to the corresponding module
                    append_element_and_comment(translation_entry, previous_entry, translations_roots[module_name])

                previous_entry = translation_entry

            except KeyError as e:
                print(f"Error processing entry #{i}: Missing key in translation entry: {e}", file=sys.stderr)
                raise
            except ValueError as e:
                print(f"Error processing entry #{i}: Error splitting module name: {e}", file=sys.stderr)
                raise
            except Exception as e:
                print(f"Error processing entry #{i}: {e}", file=sys.stderr)
                raise

        return translations_roots

    except FileNotFoundError as e:
        print(f"Error: Translation file not found: {e}", file=sys.stderr)
        raise
    except etree.XMLSyntaxError as e:
        print(f"Error: XML syntax error in translation file: {e}", file=sys.stderr)
        raise
    except Exception as e:
        print(f"Error: In \"separate_translation_to_modules\" an unexpected error occurred: {e}", file=sys.stderr)
        raise


def split_translation_files(modules_dir=None):
    """
    Splits translation files into separate files for each module and language.

    Args:
        modules_dir (str, optional): The directory containing all the modules. Defaults to None.

    """
    try:
        # Set the modules directory if not provided
        if not modules_dir:
            modules_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

        # Get the directories containing language files
        languages_dirs = get_languages_dirs(modules_dir)

        # Iterate through each language directory
        for lang_dir in languages_dirs:
            translations = separate_translation_to_modules(modules_dir, lang_dir)
            # Iterate through each module and write its translations to a file
            for module, root in translations.items():
                # Unindent the resources closing tag
                root[-1].tail = '\n'
                # Write the translation file for the module and language
                write_translation_file(modules_dir, root, module, lang_dir)

    except Exception as e:
        print(f"Error: In \"split_translation_files\" an unexpected error occurred: {e}", file=sys.stderr)
        raise


def replace_underscores(modules_dir=None):
    try:
        if not modules_dir:
            modules_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

        languages_dirs = get_languages_dirs(modules_dir)

        for lang_dir in languages_dirs:
            try:
                pattern = r'(values-\w\w)_'
                if re.search(pattern, lang_dir):
                    replacement = r'\1-r'
                    new_name = re.sub(pattern, replacement, lang_dir, 1)
                    lang_old_path = os.path.dirname(get_translation_file_path(modules_dir, 'i18n', lang_dir))
                    lang_new_path = os.path.dirname(get_translation_file_path(modules_dir, 'i18n', new_name))

                    os.rename(lang_old_path, lang_new_path)
                    print(f"Renamed {lang_old_path} to {lang_new_path}")

            except FileNotFoundError as e:
                print(f"Error: The file or directory {lang_old_path} does not exist: {e}", file=sys.stderr)
                raise
            except PermissionError as e:
                print(f"Error: Permission denied while renaming {lang_old_path}: {e}", file=sys.stderr)
                raise
            except Exception as e:
                print(f"Error: An unexpected error occurred while renaming {lang_old_path} to {lang_new_path}: {e}",
                      file=sys.stderr)
                raise

    except Exception as e:
        print(f"Error: An unexpected error occurred in rename_translations_files: {e}", file=sys.stderr)
        raise


def main():
    args = parse_arguments()
    if args.split:
        if args.replace_underscore:
            replace_underscores()
        split_translation_files()
    elif args.combine:
        combine_translation_files()


if __name__ == "__main__":
    main()
