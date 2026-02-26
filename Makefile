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

.PHONY: read_list

read_list:
	$(PYTHON) python/compare_to_mapping/make_read_list_from_mapping_for_genes.py --mapping data/pig-data-rnaseq/mapped/minimap2/H3-12936-T2.bam --gene_list gene_list.txt --gtf data/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz --od output/mapping/ --bam