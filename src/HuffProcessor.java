import java.util.*;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		/*while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}*/
		int counts[] = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);
		
		in.reset();
		writeCompressedBits(codings,in,out);
		out.close();
	}
	

	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		while(true) {
			if(in.readBits(1)==-1) break;
			String code = codings[in.readBits(BITS_PER_WORD)];
			out.writeBits(code.length(), Integer.parseInt(code,2));
		}
		String code = codings[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code,2));
	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		if(root.myValue==-1) throw new HuffException("error");
		if(root.myLeft==null && root.myRight==null) {
			out.write(1);
			out.write(BITS_PER_WORD + 1);
		}
		else {
			out.write(0);
			writeHeader(root.myLeft,out);
			writeHeader(root.myRight,out);
		}
		
	}
	private void codingHelper(HuffNode root,String s,String[] encoding) {
		if(root==null) return;
		if(root.myRight==null && root.myLeft==null) {
			encoding[root.myValue]= s;
			return;
		}
		codingHelper(root.myLeft,s+"0",encoding);
		codingHelper(root.myRight,s+"1",encoding);
	}
	private String[] makeCodingsFromTree(HuffNode root) {
		String[] encodings = new String[ALPH_SIZE + 1];
		codingHelper(root,"",encodings);
		return encodings;
	}

	private HuffNode makeTreeFromCounts(int[] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for(int k=0;k<ALPH_SIZE + 1;k++) {
			if(counts[k]>0) {
			pq.add(new HuffNode(k,counts[k],null,null));
		}}
		while(pq.size()>1) {
			HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    HuffNode t = new HuffNode(0,left.myWeight+right.myWeight,left,right);
		    pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}

	private int[] readForCounts(BitInputStream in) {
		int[] freq = new int[ALPH_SIZE + 1];
		while(true) {
			int val = in.readBits(BITS_PER_WORD);
			if(val==-1) break;
			freq[val]+=1;
		}
		freq[PSEUDO_EOF]=1;
		
		return freq;
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		/*while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}*/
		int bits = in.readBits(BITS_PER_INT);
		if(bits!=HUFF_TREE) {
			throw new HuffException("illegal header starts with "+bits);
		}
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root,in,out);
		out.close();
		
	}
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		HuffNode current = root;
		while(true) {
			int bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}
			else {
				if(bits==0) current = current.myLeft;
				else current = current.myRight;
				if(current.myLeft==null && current.myRight==null) {
					if(current.myValue==PSEUDO_EOF) {
						break;
					}
					else {
						out.writeBits(BITS_PER_WORD,current.myValue);
						current = root;
					}
				}
			}
		}
		
	}

	private HuffNode readTreeHeader(BitInputStream s) {
		int bit = s.readBits(1);
		if(bit==-1) {
			throw new HuffException("error");
		}
		if(bit==0) { 
			HuffNode left = readTreeHeader(s);
			HuffNode right = readTreeHeader(s);
			return new HuffNode(0,0,left,right);
		}
		else {
			int value = s.readBits(BITS_PER_WORD+1);
			return new HuffNode(value,0,null,null);
		}
	}
	
	 
}