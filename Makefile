clean_translations_temp_directory:
	rm -rf i18n/

translation_requirements:
	pip3 install -r i18n_scripts/requirements.txt

pull_translations: clean_translations_temp_directory
	atlas pull $(ATLAS_OPTIONS) translations/openedx-app-android/i18n:i18n
	python3 i18n_scripts/translation.py --split --replace-underscore

extract_translations: clean_translations_temp_directory
	python3 i18n_scripts/translation.py --combine

validate_english_plurals:
	@if git grep 'quantity' -- '**/res/values/strings.xml' | grep -E 'quantity=.(zero|two|few|many)'; then \
		echo ""; \
		echo ""; \
		echo "Error: Found invalid plurals in the files listed above."; \
		echo "       Please only use 'one' and 'other' in English strings.xml files,"; \
		echo "       otherwise Transifex fails to parse them."; \
		echo ""; \
		exit 1; \
	else \
		echo "strings.xml files are valid."; \
	fi
