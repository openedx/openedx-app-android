clean_translations_temp_directory:
	rm -rf i18n/

translation_requirements:
	pip3 install -r i18n_scripts/requirements.txt

pull_translations: clean_translations_temp_directory
	atlas pull $(ATLAS_OPTIONS) translations/openedx-app-android/i18n:i18n
	python3 i18n_scripts/translation.py --split --replace-underscore

extract_translations: clean_translations_temp_directory
	python3 i18n_scripts/translation.py --combine
