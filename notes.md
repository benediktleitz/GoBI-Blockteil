```toc
```
## 23.02

### wichtig

### und

### richtig!

1. (elena, ida), (thomas, eren), (mohit, nikos)
2. (3), (k,v), (wir)
3. (Tolga, Mo)
4.
5. (3)


### Besprechung/Planung
Input: fastq + stelle (als koordinate = chr, index, offset + genom)
Input parameter: k, offset for kmer

split input sequence into kmers
reads into kmers
count exact matches/overlap of kmers
threshold

KMer klasse (hash, equals, byte array)
input sequence into kmer hashset
read reader
read handler static




### fragen
output format (ids, liste, reads, fastq) (= input für gtamap)
paired-end:
kann es oder nicht, sollen wir die info extra behandeln
(beide müssen passen, eine muss passen,...)
gleichmäßige verteilung mutationen (wie verhindern dass nicht rausgefiltert wird 160, 20)

## 24.02
### sam
reference offset 1, read offset k

read processing needs to be quick

index: mehrere k theoretisch (später)

worst case 10 mutationen, throw out one with 10 mutations
keinen fall false negatives : solange wir klar definieren was wir durchlassen, sind fn ok
worst case definieren

k variabel halten (10 pass or fail -> 5)

report: übersicht, was passiert laufzeit, was lassen wir durch, statistik
pro h1 h2 lassen wir n durch, k größer schnellere laufzeit aber weniger durch

k=20 maybe 1 match enough  aber sehen wir durch gridsearch
parameter aber auch wahl target gene schiwierig
man kann aber auch information mit reinnehmen ob paraloges gen (mehr kopien, mehr matches)
und repeats
5 matches aber in repeat region schwierig
repeat region strengerer threshold

sample abhängig
framework where these parameters (gene), choose range gridsearch, report for parameter range

gute gene zum testen:
repeat masker (detect repeats for human, maybe pig)
protein coding genes ("only" 20k)
do we want one target gene -> filter
or filter for some or all (too much, index too large)

mapper alles aufm server
sam macht und gibt uns

tp, fp, tn, fn zimmer (!!!) no truth really, "how many align with mapper"

long: max 32 fine good enough
für jeden read: liste an genen (best)
size/runtime/presentation of results

start with any gene
il10
ccr9 (bit shorter, some transcripts, exons)

for all genes good

output:
fastq (one gene)
matrix maybe read id, boolean yes no for gene
sort genes, über index
not als gene id

sparse matrizen effizient file formats

paired end:
variabel, einer muss passen oder beide
eher beide , aber option für eins

htsdk kann mit block gzip

### plan
- 1 gen
- genau 1 k
  default runs:
- gtf + fasta + fai -> all protein coding genes
- stelle -> nur diese stelle
- gtf + gene ids -> only these genes

output 1 fastq

### aufgaben für heute
reader schreiben (fastq = reads, reference for one gene = fasta + fai, cmdlinereader)
initialise (reference kmer set creator (sequenz))
kmer filtern (forward and reverse read -> yes or no)
main

## 25.02

fastq output
count output (maybe optional machen)
only gene list
million read/minute

### plan from here

parameter testen
bash script for grid search (1 gen)
for each k, offset, threshold output tsv 
vergleiche mit mapping output (von sam) ipynb
sam/bam file script reads mapped to that region
merge files and compare



## Todos
check N in sequences (or any char other than A,C,G,T)
check chunk size for efficient parallelization


## 27.02

paired end (correct)
repeats
check N in sequences (or any char other than A,C,G,T)

nach parameter (check chunk size for efficient parallelization)

python script anpassen (input transcript ids)

python script mehr vektorisieren (?) 
bessere engine zum einlesen

first plots




## 02.03 

### Sam Gespräch

Wie sollen wir N handeln -> Wird es langsamer machen, wegen 2 if statements pro k-mer 
- Wir können es so lassen, hat zwar kleinen bias, aber Geschwindigkeit ist wichtiger
  
13.5M reads/1 min mit -rna, -or, k=12, o=12, t=24/36, 5 Gene
-> Hohes match zu mapping Ergebnis und min 80% rausgefiltert
Wie mit non-protein coding Genen umgehen? (Wenn wir ne Gen liste bekommen protein-coding ignorieren?)
- Alle Gene der Liste nehmen regardless
SNP Liste als optionalen Input -> Welches Format? Vor allem bei mehreren Genen muss man wissen, welcher SNP Kmer für welches Gen da ist
- kommt noch die nächste Tage

Output Ordner Berechtigungen


### ToDos:

Gridsearch über k=10 bis k=15 mit or und rna
Gen liste -> Alle behandeln und ned nur protein_coding

## IMPORTANT (REALLY!)

read directions, what is fw, where do i have to use reverse complement?


## 03.03.

### Was können wir noch tun?
- Repeat Regionen/ Paraloge Gene anders behandeln
- Statt tsv effizienteren output (z.B. in Binary (und dann len von read id und danach das bitset für die Gene), oder simpler (was wahrscheinlich kürzer zum lesen braucht): read_id\tgene_id für jede Spalte und einfach jeden hit in einer Zeile)
- check chunk size for efficient parallelization
- Quality scores betrachten

### Wie können wir was auswerten?
- Runtime plotten: Über k, über offset, über threshold, chunksize (also immer eins variabel und die anderen fix, maybe für jedes and und or)
- Memory plotten: Über unterschiedliche Anzahl von Genen mit fixem k, offset und threshold, maybe and und or unterscheiden -> Maybe Gen länge irgendwie zum Normalisieren auch benutzen oder beachten
-> Memory und runtime am besten für unterschiedliche fastqs und dann nach read count normalisieren
- Filtered, not_filtered (maybe mit zwei linien und zwei Achsen (eine Links und eine rechts)) über k, offset und threshold (eins variabel, die anderen fix?, auch für jedes and und or)

-> Für alles auch zwischen RNA und DNA Modus unterscheiden

