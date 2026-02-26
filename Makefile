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

.PHONY: read_list filter compare

read_list:
	$(PYTHON) python/compare_to_mapping/make_read_list_from_mapping_for_genes.py --mapping output/bams/minimap2/H6-12939-T2.sorted.bam --gene_list output/filter_quality_analysis/H6/Test1/gene_list.txt --gtf data/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz --od output/filter_quality_analysis/H6/Test1/read_lists_mapping

filter:
	java -jar gta_filter.jar -fw data/pig-data-rnaseq/H6-12939-T2_R1_001.fastq.gz -rw data/pig-data-rnaseq/H6-12939-T2_R3_001.fastq.gz -k 15 -offset 15 -threshold 15 -fasta data/pig-genome/Sus_scrofa.Sscrofa11.1.dna.toplevel.fa.gz -od output/filter_quality_analysis/H6/Test1 -gtf data/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz -genes output/filter_quality_analysis/H6/Test1/gene_list.txt -counts

compare:
	$(PYTHON) python/compare_to_mapping/compare_filter_to_mapping.py --read-lists output/filter_quality_analysis/H6/Test1/read_lists_mapping --filter-result output/filter_quality_analysis/H6/Test1/read2gene_matrix.tsv --od output/filter_quality_analysis/H6/Test1/
