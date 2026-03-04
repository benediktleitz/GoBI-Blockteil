import pandas as pd
import argparse
import os
import pyranges as pr


def main(args):
    gtf = pr.read_gtf(args.gtf)
    gtf = gtf[gtf.Feature == "gene"]
    if args.pc:
        gtf = gtf[gtf.gene_biotype == "protein_coding"]
    gene_list = gtf.gene_id.unique()
    selected_genes = pd.Series(gene_list).sample(n=int(args.num)).tolist()
    with open(args.o, 'w') as f:
        for gene in selected_genes:
            f.write(gene + "\n")
    

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Create a gene list file for a random selection of genes from the input gtf."
    )
    parser.add_argument("--gtf", required=True,
                        help="Path to the input gtf file")
    parser.add_argument("--num", required=True,
                        help="Number of random genes to select.")
    parser.add_argument("--o", required=True,
                        help="Path to the output file.")
    parser.add_argument("--pc", required=False,
                        help="Only use protein coding genes.", action="store_true")

    args = parser.parse_args()
    main(args)