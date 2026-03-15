# Include config variables
include config.mk

.PHONY: setup_venv

setup_venv:
	@echo "Removing old venv if it exists..."
	rm -rf $(VENV_DIR)
	@echo "Creating new venv with $(PYTHON)..."
	$(PYTHON) -m venv $(VENV_DIR)
	@echo "Activating venv and upgrading pip, setuptools, wheel..."
	. $(VENV_DIR)/bin/activate && $(PIP) install --upgrade pip setuptools wheel
	@echo "Installing requirements..."
	. $(VENV_DIR)/bin/activate && $(PIP) install -r $(REQUIREMENTS)
	@echo "Done!"