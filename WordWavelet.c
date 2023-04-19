#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct CompressedTrieNode{
	char *string;
	int strlen;
	int wordNum;
	char isWord;
	struct CompressedTrieNode *suffix[26];
} CompressedTrieNode;

typedef struct CompressedTrie{
	int numWords;
	CompressedTrieNode *suffix[26];

} CompressedTrie;

typedef struct WordWaveletTreeNode{
	int numBits;
	char *bitVector;
	struct WordWaveletTreeNode *left, *right;
} WordWaveletTreeNode;

typedef struct WordWaveletTree{
	CompressedTrie *library;
	WordWaveletTreeNode *root;
} WordWaveletTree;

CompressedTrieNode *buildCompressedTrieNode(char *string, int strlen){
	CompressedTrieNode *n = calloc(1, sizeof(CompressedTrieNode));
	n->strlen = strlen;
	n->string = malloc(sizeof(char) * (n->strlen + 1));
	strcpy(n->string, string);

	return n;
}

CompressedTrieNode *buildCompressedTrieHelper(CompressedTrieNode *n ,char *string){
	
	int index = 0;
	// check characters until you find a mismatch or end of both strings
	while(string[index] == n->string[index] && string[index] != '\0'){
		index++;
	}
	if(string[index] == '\0'){
		// Case 1: string = n->string; flag node n as a word and update word count
		if(n->string[index] == '\0'){
			// already saved this word
			if(n->isWord){
				return n;
			}
			// update the word bank
			n->isWord = 1;
			return n;
		}
		// Case 2: string is a prefix of n->string; build a node for string and make n a suffix node of it
		CompressedTrieNode *new = buildCompressedTrieNode(string, index);
		new->isWord = 1;

		char *newString = malloc(sizeof(char) * (n->strlen - index + 1));
		char *tmp = n->string;
		strcpy(newString, &(n->string[index]));
		n->string = newString;
		free(tmp);

		new->suffix[n->string[0] - 'a'] = n;
		return new;
	}
	else{
		// Case 3: n->string is a prefix of string; pass along the suffix of string to the appropriate node
		if(n->string[index] == '\0'){
			int i = string[index] - 'a';
			// Make string a suffix of n
			if(n->suffix[i] == NULL){
				n->suffix[i] = buildCompressedTrieNode(&(string[index]), strlen(&(string[index])));
				n->suffix[i]->isWord = 1;
				return n;
			}
			// pass it along
			n->suffix[i] = buildCompressedTrieHelper(n->suffix[i], &(string[index]));

			return n;
		}
		// Case 4: n->string and string share a common prefix; build a node for the prefix and for string's suffix
		// then make n and string suffixes of the new prefix node
		char *nSuffix = malloc(sizeof(char) * (n->strlen - index + 1));
		strcpy(nSuffix, &(n->string[index]));
		char *tmp = n->string;
		n->string = nSuffix;
		n->strlen -= index;
		free(tmp);

		CompressedTrieNode *new = buildCompressedTrieNode(&(string[index]), strlen(&(string[index])));
		new->isWord = 1;

		string[index] = '\0';
		CompressedTrieNode *prefix = buildCompressedTrieNode(string, index);

		prefix->suffix[n->string[0] - 'a'] = n;
		prefix->suffix[new->string[0] - 'a'] = new;

		return prefix;

	}

}

int assignWordNums(CompressedTrieNode *n, int wordCount){
	if(n == NULL){
		return wordCount;
	}

	if(n->isWord){
		wordCount++;
	}
	n->wordNum = wordCount;
	for(int i = 0; i < 26; i++){
		wordCount = assignWordNums(n->suffix[i], wordCount);
	}
	return wordCount;
}

CompressedTrie *buildCompressedTrie(FILE *fp){
	CompressedTrie *t = calloc(1, sizeof(CompressedTrie));
	char buffer[1000];
	int index;
	while(fscanf(fp, "%s", buffer) != EOF){
		index = buffer[0] - 'a';
		if(t->suffix[index] == NULL){
			t->suffix[index] = buildCompressedTrieNode(buffer, strlen(buffer));
			t->suffix[index]->isWord = 1;
		}
		else{
			t->suffix[index] = buildCompressedTrieHelper(t->suffix[index], buffer);
		}
	}
	for(int i = 0; i < 26; i++){
		t->numWords = assignWordNums(t->suffix[i], t->numWords);
	}
	return t;
}

WordWaveletTree *buildWordWaveletTree(char *filename){
	FILE *fp = fopen(filename, "r");
	if(fp ==NULL){
		fprintf(stderr, "Error: File not found\n");
		exit(2);
	}
	WordWaveletTree *w = malloc(sizeof(WordWaveletTree));
	w->library = buildCompressedTrie(fp);
	fclose(fp);
	return w;
}

void printTrieHelper(CompressedTrieNode *n, char buffer[], int index){
	if(n == NULL){
		return;
	}
	printf("%s %d\n", n->string, n->isWord);
	strcpy(&(buffer[index]), n->string);
	if(n->isWord){
		printf("%3d: %s\n", n->wordNum, buffer);
	}

	for(int i = 0; i < 26; i++){
		printTrieHelper(n->suffix[i], buffer, index + n->strlen);
	}
}

void printTrie(CompressedTrie *t){
	char buffer[1000];
	for(int i = 0; i < 26; i++){
		printTrieHelper(t->suffix[i], buffer, 0);
	}
}
void printBinaries(unsigned char c){
	printf("%u", c >> 7);
	printf("%u", (c >> 6) % 2);
	printf("%u", (c >> 5) % 2);
	printf("%u", (c >> 4) % 2);
	printf("%u", (c >> 3) % 2);
	printf("%u", (c >> 2) % 2);
	printf("%u", (c >> 1) % 2);
	printf("%u\n", c % 2);

}
// for compression, each n->string will be compressed to 5 bits per
// char plus one final bit for n->isWord
void saveTrie(FILE *fp, CompressedTrieNode *n){
	if(n == NULL){
		return;
	}
	
	unsigned char c = 0, d;
	// total bits in use in char c
	unsigned int bits = 0;
	// first store how many bits will be used for storage
	// This will be stored in a single byte as a char
	fprintf(fp, "%d ", n->strlen * 5);
	printf("%d\n", n->strlen*5);
	printf("%s\n", n->string);
	for(int i = 0; i < n->strlen; i++){
		d = n->string[i];
		// We can store the whole char in the remainder of the byte
		if(bits < 3){
			c += (d -'a' + 1) << (3 - bits);
			bits += 5;
		}
		// special case; char c is guaranteed to be completely full with no overflow
		else if(bits == 3){
			c += (d -'a' + 1);
			fprintf(fp, "%c", c);
			printBinaries(c);
			c = 0;
			bits = 0;
		}
		// partially store what you can, print c, then store the rest
		else{
			// 8 - bits gives us how many of our 5-bit letter get saved
			// 5 - (8-bits) determines how much we shave off
			c += (d - 'a' + 1) >> (5 - (8 - bits));
			fprintf(fp, "%c", c);
						printBinaries(c);

			// 8 - bits: how many bits were originally saved
			// 5 - (8 - bits) = bits - 3: number of bits not previously saved
			// 8 - (bits - 3) = 11 - bits; how far we have to shift the remaining bits to reach the MSB of c
			c = (d-'a' + 1) << (11 - bits);
			bits = bits - 3;
		}
	}
	// add a 1 or 0 at the end for n->isWord
	// Ideally, we'd store that bit at the end of the final byte
	// special case: no remaining bits Have to use a new byte
	if(bits == 0){
		fprintf(fp, "%c", n->isWord ? 128 : 0);
			printBinaries(n->isWord ? 128 : 0);

	}
	else{
		if(n->isWord){
			c += 1 << (7 - bits);
		}
		fprintf(fp, "%c", c);
			printBinaries(c);

	}

	for(int i = 0; i < 26; i++){
		saveTrie(fp, n->suffix[i]);
	}
}
void save(WordWaveletTree *tree){
	FILE *fp = fopen("save.txt", "w");
	for(int i = 0; i < 26; i++){
		saveTrie(fp, tree->library->suffix[i]);
	}

	fclose(fp);
}
int main(int args, char **argv){
	if(args < 2){
		fprintf(stderr, "No file given at startup\n");
		exit(1);
	}

	WordWaveletTree *tree = buildWordWaveletTree(argv[1]);
	printTrie(tree->library);

	save(tree);
}