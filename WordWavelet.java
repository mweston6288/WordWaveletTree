import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.nio.charset.*;
public class WordWavelet{
	public static void main(String[] args) throws Exception{
		FileWriter fp = new FileWriter(new File("save.txt"));
		char c = 65535;		
		fp.write(c);
		fp.close();
		System.exit(0);
		if(args.length < 1){
			System.out.println("Requires file name when run");
			System.exit(1);
		}

		try{
			WaveletTree root = new WaveletTree(args[0]);
			root.save();
			root.restore();	
			// WaveletTree root = WaveletTree.build("save.txt");
			// root.restore();
		}
		catch (FileNotFoundException e){
			System.out.println("File not found: " + args[0]);
			System.exit(1);
		}

		catch (IOException e){
			System.out.println("Output error");
			System.exit(2);		
		}
	}

	
}
class WaveletTree{

	CompressedTrie list;
	WaveletTreeNode root;
	long numBits;

	public WaveletTree(){
		list = new CompressedTrie();
		root = null;
		numBits = 0;
	}

	public WaveletTree(String filename) throws FileNotFoundException{
		this.list = new CompressedTrie();
		
		Scanner scan = new Scanner(new File(filename));
		String n;

		while (scan.hasNext()){
			n = scan.next();

			list.addWord(n);
		}
		scan.close();
		list.printTrie();
		this.root = new WaveletTreeNode(list, filename);
		this.numBits = this.root.numBits;
	}

	// TODO: Update with CompressedTrie
	// public static WaveletTree build(String filename){
	// 	int n;
	// 	WaveletTree w = new WaveletTree();
	// 	try{
	// 		Scanner scan = new Scanner(new File(filename));

	// 		n = scan.nextInt();
	// 		for(int i = 0; i < n; i++){
	// 			w.list.add(scan.next());
	// 		}
	// 		scan.nextLine();
	// 		w.root = new WaveletTreeNode(scan);
	// 		w.numBits = w.root.numBits;
	// 	}
	// 	catch(FileNotFoundException e){
	// 		System.out.println("File not found: " + filename);
	// 		System.exit(1);
	// 	}
	// 	return w;
	// }



	public void save() throws IOException{
		FileWriter fp = new FileWriter(new File("save.txt"), StandardCharsets.US_ASCII);
		//fp.write(this.list.numWords + "\n");

		list.writeToFile(fp);

		writeVectors(fp, root);

		fp.close();
	}	

	private void writeVectors(FileWriter fp, WaveletTreeNode root)throws IOException{
		if(root == null){
			fp.write("\n");
			return;
		}
		fp.write(root.numBits+" ");
		for(int i = 0; i < root.numBits / 8; i++){
			fp.write(root.bits[i]);
		}
		fp.write("\n");
		writeVectors(fp, root.left);
		writeVectors(fp, root.right);

	}
	// TODO: add Compressed Trie version
	public void restore()throws IOException{
		FileWriter fp = new FileWriter("res.txt");
		for(int i = 0; i < this.numBits; i++){
			restoreHelper(fp, this.root, 1, this.list.numWords, i);
		}
		fp.close();
	}

	private void restoreHelper(FileWriter fp, WaveletTreeNode root, int low, int high, int bitCount)throws IOException{
		System.out.println(low + " " + high);
		if(low >= high){
			fp.write(this.list.getNthString(low) + " ");
			return;
		}
		int bitshift = 7;
		int bytes = 0;
		int one = 0;
		int zero = 0;
		for(int i = 0; i < bitCount; i++){
			if((root.bits[bytes] >>> bitshift) % 2 == 1){
				one++;
			}
			else{
				zero++;
			}
			bitshift--;
			if (bitshift < 0){
				bytes++;
				bitshift = 7;
			}

		}
		if((root.bits[bytes] >>> bitshift) % 2 == 1){
			restoreHelper(fp, root.right, (low+high) / 2 + 1, high, one);
		}
		else{
			restoreHelper(fp, root.left, low, (low+high) / 2 , zero);
		}
	}

	static class WaveletTreeNode{
		byte[] bits;
		int numBits;
		WaveletTreeNode left,right;

		public WaveletTreeNode(CompressedTrie list, String filename) throws FileNotFoundException{
			this(list, filename, 1, list.numWords);
		}

		public WaveletTreeNode(CompressedTrie list, String filename, int low, int high) throws FileNotFoundException{
			if(low >= high){
				throw new NullPointerException();
			}
			this.bits = new byte[100];
			this.numBits = 0;
			int mid = (low + high) / 2;
			int bitshift = 7, bytes=0;
			Scanner scan = new Scanner(new File(filename));
			
			String lowS = list.getNthString(low);
			String midS = list.getNthString(mid);
			String highS = list.getNthString(high);
			while(scan.hasNext()){
				String s = scan.next();
				// check if word is in allowed range
				if(s.compareTo(highS)<=0 && s.compareTo(lowS) >= 0){
					if((s.compareTo(midS)) > 0){
						this.bits[bytes] += 1 << bitshift;
					}
					this.numBits++;
					bitshift--;
					if(bitshift < 0){
						bytes++;
						bitshift = 7;
					}
					if(bytes >= this.bits.length){
						this.bits = Arrays.copyOf(this.bits, this.bits.length*2);
					}
				}

			}
			printBitVector();
			scan.close();
			try{
				this.left = new WaveletTreeNode(list, filename, low, mid);
			}
			catch(NullPointerException e){
				this.left = null;	
			}


			try{
				this.right = new WaveletTreeNode(list, filename, mid+1, high);
			}
			catch(NullPointerException e){
				this.right = null;	
			}

		}
	
		public WaveletTreeNode(Scanner scan){
			String s = scan.nextLine();

			Scanner s2 = new Scanner(s);

			if(s2.hasNext()){
				numBits = s2.nextInt();
				bits = new byte[numBits / 8 + 1];
				for(int i = 0; i < bits.length; i++){
					//bits[i] = s2.next();

				}
				s2.close();
				try{
					left = new WaveletTreeNode(scan);
				}
				catch(NullPointerException e){
					left = null;
				}

				try{
					right = new WaveletTreeNode(scan);
				}
				catch(NullPointerException e){
					right = null;
				}
			}
			else{
				s2.close();
				throw new NullPointerException();
			}
		}

		public void printBitVector(){
			int bytes = 0;
			int bitshift = 7;
			for(int i = 0; i < this.numBits; i++){
				System.out.print((bits[bytes] >>> bitshift) % 2);
				bitshift--;
				if(bitshift < 0){
					bytes++;
					bitshift=7;

				}
			}
			System.out.println();
		}
	}

	static class StringCompare implements Comparator<String>{
		public int compare(String a, String b){
			return a.compareTo(b);
		}
	}
}

class CompressedTrie{

	int numWords;
	CompressedTrieNode root;

	public CompressedTrie(){
		root = new CompressedTrieNode();
		numWords = 0;
	}

	public void addWord(String entry){
		int index = entry.charAt(0) - 'a';
		if(root.suffix[index] == null){
			root.suffix[index] = new CompressedTrieNode(entry);
			numWords++;
			root.numWords++;
			return;
		}
		int r = root.suffix[index].numWords;
		root.suffix[index] = root.suffix[index].addWord(entry);
		if(r != root.suffix[index].numWords){
			numWords++;
			root.numWords++;
		}
	}

	public void printTrie(){
		printTrieHelper(root, root.string);
	}

	private void printTrieHelper(CompressedTrieNode n, String s){
		s = s.concat(n.string);
		if(n.isWord){
			System.out.println(s);
		}

		for(int i = 0; i < n.suffix.length; i++){
			if(n.suffix[i] != null){
				printTrieHelper(n.suffix[i], s);
			}
		}
	}

	public void writeToFile(FileWriter fp) throws IOException{
		for(int i = 0; i < root.suffix.length; i++){
			writeToFileHelper(fp, root.suffix[i]);
		}
	}

	public void writeToFileHelper(FileWriter fp, CompressedTrieNode n) throws IOException{
		if(n == null){
			return;
		}
		char c,d;
		for(int i = 0; i < n.string.length(); i++){
			//d = Character.  n.string.charAt(i) - 'a' + 1;
		}
		fp.write(n.string +" " + (n.isWord ? "1" : "0") +"\n");
		for(int i = 0; i < n.suffix.length; i++){
			writeToFileHelper(fp, n.suffix[i]);
		}
		fp.write("\n");
	}

	public String getNthString(int n){
		String s = "";
		CompressedTrieNode node = root;

		int i = 0, j;
		while(i <= n){

			for(j = 0; j < node.suffix.length; j++){
				if (node.suffix[j] != null){
					if(i + node.suffix[j].numWords >= n){
						node = node.suffix[j];
						s = s.concat(node.string);
						break;
					}
					i += node.suffix[j].numWords;
				}
			}
			if(node.isWord){
				i++;
			}
		}
		
		return s;
	}

	class CompressedTrieNode{
		String string;
		// number of words branching from this node (including itself if isWord = true)
		int numWords;
		boolean isWord;
		CompressedTrieNode [] suffix;

		public CompressedTrieNode(){
			string = "";
			isWord = false;
			numWords = 0;
			suffix = new CompressedTrieNode[26];
		}

		public CompressedTrieNode(String entry){
			string = entry;
			isWord = true;
			numWords = 1;
			suffix = new CompressedTrieNode[26];
		}

		public CompressedTrieNode addWord(String entry){
			int i = 0;
			try{
				// compare each char one-by-one
				for(; this.string.charAt(i) == entry.charAt(i); i++);
			}
			// One or both strings ran out of bounds, meaning one is a prefix to the other or they're equal
			catch (IndexOutOfBoundsException e){
				// entry is a prefix to this.string
				// build a new node for entry and have this branch off it
				if(entry.length() < this.string.length()){
					CompressedTrieNode n = new CompressedTrieNode(entry);
					n.suffix[this.string.charAt(i) - 'a'] = this;
					n.numWords = this.numWords + 1;
					
					this.string = this.string.substring(i);
					return n;
				}
				// this.string is a prefix to entry
				// pass entry along to the proper suffix
				else if(entry.length() > this.string.length()){
					int index = entry.charAt(i) - 'a';
					// This word definitely never appeared before
					if(this.suffix[index] == null){
						this.suffix[index] = new CompressedTrieNode(entry.substring(i));
						this.numWords++;
						return this;
					}
					// we do not yet know if entry is a new word
					// if r is different after insertion, that means the suffix node accepted it as a new word
					else{
						int r = this.suffix[index].numWords;
						this.suffix[index] = this.suffix[index].addWord(entry.substring(i));
						if (r != this.suffix[index].numWords){
							this.numWords++;
						}
						return this;
					}
				}
				// they're equal. Do nothing
				else {
					if(!this.isWord){
						this.isWord = true;
						this.numWords++;
					}
					return this;
				}
			}
			// split this.string and make two new nodes
			CompressedTrieNode n = new CompressedTrieNode(entry.substring(0, i));
			CompressedTrieNode n2 = new CompressedTrieNode(entry.substring(i));

			this.string = this.string.substring(i);

			n.isWord = false;
			n.suffix[this.string.charAt(0) - 'a'] = this;
			n.suffix[n2.string.charAt(0) - 'a'] = n2;

			n.numWords = this.numWords + 1;
			return n;
		}
	}
}