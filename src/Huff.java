import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Huff implements ITreeMaker, IHuffConstants, IHuffEncoder, 
IHuffHeader, IHuffModel{
	// IHuffModel, //IHuffHeader {

	private Map<Integer, Integer> count;
	private Map<Integer, String> encoding;
	private HuffTree tree;

	public Huff() {
		count = new HashMap<Integer, Integer>();
		encoding = new HashMap<Integer, String>();
		tree = null;
	}

	/**
	 * Return the Huffman/coding tree.
	 * 
	 * @return the Huffman tree
	 */
	public HuffTree makeHuffTree(InputStream stream) throws IOException {
		// get map first
		ICharCounter cc = new CharCounter();
		cc.countAll(stream);
		count = cc.getTable();
		MinHeap Hheap = buildHeap();
		// build heap from map
		// build tree from heap
		if (Hheap.heapsize() > 1) {
			HuffTree tmp1, tmp2, tmp3 = null;
			while (Hheap.heapsize() > 1) { // While two items left
				tmp1 = (HuffTree) Hheap.removemin();
				tmp2 = (HuffTree) Hheap.removemin();
				tmp3 = new HuffTree(tmp1.root(), tmp2.root(), tmp1.weight() + tmp2.weight());
				Hheap.insert(tmp3); // Return new tree to heap
			}
			tree = tmp3;
			return tree; // Return the tree
		} else {
			tree = (HuffTree) Hheap.removemin();
			return tree;
		}
	}

	/**
	 * build heap from count map first for building tree later
	 * 
	 * @return
	 */
	public MinHeap buildHeap() {
		// create a new array;
		HuffTree[] h = new HuffTree[ALPH_SIZE + 1];
		// create a new heap
		MinHeap Hheap = new MinHeap(h, 0, ALPH_SIZE + 1);
		// create new node, put it in heap
		for (int key : count.keySet()) {
			HuffTree newNode = new HuffTree(key, count.get(key));
			Hheap.insert(newNode);
		}
		// insert PSEUDO_EOF
		HuffTree newNode = new HuffTree(PSEUDO_EOF, 1);
		Hheap.insert(newNode);
		return Hheap;
	}

	/**
	 * Initialize state from a tree, the tree is obtained from the treeMaker.
	 * 
	 * @return the map of chars/encoding
	 */
	public Map<Integer, String> makeTable() {
		makeTable(tree.root(), "");
		return encoding;
	}

	/**
	 * helper method to build table recursively
	 * 
	 * @param node
	 * @param path
	 */
	private void makeTable(IHuffBaseNode node, String path) {
		if (node.isLeaf()) {
			encoding.put(((HuffLeafNode) node).element(), path);
			// System.out.println(path);
			return;
		} else {
			IHuffBaseNode left = ((HuffInternalNode) node).left();
			IHuffBaseNode right = ((HuffInternalNode) node).right();
			makeTable(left, path + "0");
			makeTable(right, path + "1");
			return;
		}
	}

	/**
	 * Returns coding, e.g., "010111" for specified chunk/character. It is an error
	 * to call this method before makeTable has been called.
	 * 
	 * @param i
	 *            is the chunk for which the coding is returned
	 * @return the huff encoding for the specified chunk
	 */
	public String getCode(int i) {
		return encoding.get(i);
	}

	/**
	 * @return a map of all characters and their frequency
	 */
	public Map<Integer, Integer> showCounts() {
		return count;
	}

	/**
	 * Write a compressed version of the data read by the InputStream parameter, --
	 * if the stream is not the same as the stream last passed to initialize, then
	 * compression won't be optimal, but will still work. If force is false,
	 * compression only occurs if it saves space. If force is true compression
	 * results even if no bits are saved.
	 * 
	 * @param inFile
	 *            is the input stream to be compressed
	 * @param outFile
	 *            specifies the OutputStream/file to be written with compressed data
	 * @param force
	 *            indicates if compression forced
	 * @return the size of the compressed file
	 * @throws FileNotFoundException
	 */
	public int write(String inFile, String outFile, boolean force) {
		// create new stream for count original size
		BitInputStream bitin1 = new BitInputStream(inFile);
		// create new stream for making huff tree
		BitInputStream bitin2 = new BitInputStream(inFile);
		// count size of original file
		CharCounter cc = new CharCounter();
		int originalSize = 0;
		int compress = 0;
		try {
			originalSize = cc.countAll(bitin1) * 8;
			// make tree
			makeHuffTree(bitin2);
			// make encoding table
			makeTable();
			// close stream
			bitin1.close();
			bitin2.close();
			// count size of compress file
			compress = compressSize(inFile);
			// if force or indeed compressed
			if (force || originalSize > compress) {
				writeToFile(inFile, outFile);
			}
			else compress = 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// return the size of the compressed file
		return compress;
	}

	/**
	 * helper method to write to file
	 * 
	 * @param inFile
	 * @param outFile
	 * @throws IOException
	 * @return the size of the compressed file
	 */
	public int writeToFile(String inFile, String outFile) throws IOException {
		int size = 0;
		// write header and record size
		BitInputStream bitin = new BitInputStream(inFile);
		BitOutputStream bitout = new BitOutputStream(outFile);
		writeHeader(bitout);
		size += headerSize();
		int inbits;
		while ((inbits = bitin.read(BITS_PER_WORD)) != -1) {
			String code = encoding.get(inbits);
			for (int i = 0; i < code.length(); i++) {
				// increment size
				size++;
				if (code.charAt(i) == '1')
					bitout.write(1, 1);
				else
					bitout.write(1, 0);
			}
		}
		// write EOF and update size
		String code = encoding.get(PSEUDO_EOF);
		for (int i = 0; i < code.length(); i++) {
			// increment size
			size++;
			if (code.charAt(i) == '1')
				bitout.write(1, 1);
			else
				bitout.write(1, 0);
		}
		bitin.close();
		bitout.close();
		return size;
	}

	/**
	 * helper method to calculate compressed file size
	 * 
	 * @param inFile
	 * @returnthe size of the compressed file
	 * @throws IOException
	 */
	public int compressSize(String inFile) throws IOException {
		int size = 0;
		// add header size
		size += headerSize();
		// add code size
		BitInputStream bitin = new BitInputStream(inFile);
		int inbits;
		while ((inbits = bitin.read(BITS_PER_WORD)) != -1) {
			// put writes one character
			size += encoding.get(inbits).length();
		}
		// add EOF
		size += (BITS_PER_WORD + 1);
		bitin.close();
		return size;
	}

	/**
	 * Uncompress a previously compressed file.
	 * 
	 * @param inFile
	 *            is the compressed file to be uncompressed
	 * @param outFile
	 *            is where the uncompressed bits will be written
	 * @return the size of the uncompressed file
	 * @throws IOException
	 */
	public int uncompress(String inFile, String outFile) {
		BitInputStream bitin = new BitInputStream(inFile);
		BitOutputStream bitout = new BitOutputStream(outFile);
		// read tree
		HuffTree tree;
		// read code
		int size = 0;
		try {
			tree = readHeader(bitin);
			size = readEncoding(tree, bitin, bitout);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bitin.close();
		bitout.close();
		return size;
	}

	/**
	 * 
	 * @param tree
	 * @param bitin
	 * @param bitout
	 * @return the size of the uncompressed file
	 * @throws IOException 
	 */
	public int readEncoding(HuffTree tree, BitInputStream bitin, BitOutputStream bitout) 
			throws IOException {
		int size = 0;
		int bits;
		IHuffBaseNode node = tree.root();
		while (true) {
			bits = bitin.read(1);
			if (bits == -1) {
				throw new IOException("unexpected end of input file");
			}
			else {
				// read a 0, go left in tree
				if ((bits & 1) == 0) node = ((HuffInternalNode)node).left();
				// read a 1, go right in tree
				else node = ((HuffInternalNode)node).right();

				if (node.isLeaf()) {
					if (((HuffLeafNode)node).element() == PSEUDO_EOF) 
						break; // out of loop
					else {
						bitout.write(((HuffLeafNode)node).element());
						// increment size
						size += 8;
					}
					// make node return to root
					node = tree.root();
				}
			}
		}
		return size;
	}

	/**
	 * The number of bits in the header using the implementation, including the
	 * magic number presumably stored.
	 * 
	 * @return the number of bits in the header
	 */
	public int headerSize() {
		// headerSize +32 because of magic number
		int headerSize = 0;
		headerSize += BITS_PER_INT;
		headerSize += treeSize(tree.root());
		return headerSize;
	}

	/**
	 * helper function to calculate size
	 * 
	 * @param node
	 * @return return huff tree size
	 */
	private int treeSize(IHuffBaseNode node) {
		if (node.isLeaf()) {
			// write 1 and write 9 bit to file
			return 1 + (BITS_PER_WORD + 1);
		} else {
			IHuffBaseNode left = ((HuffInternalNode) node).left();
			IHuffBaseNode right = ((HuffInternalNode) node).right();
			// write 0 indicating internal node
			// write left and right
			return treeSize(left) + treeSize(right) + 1;
		}
	}

	/**
	 * Write the header, including magic number and all bits needed to reconstruct a
	 * tree, e.g., using <code>readHeader</code>.
	 * 
	 * @param out
	 *            is where the header is written
	 * @return the size of the header
	 */
	public int writeHeader(BitOutputStream out) {
		out.write(BITS_PER_INT, MAGIC_NUMBER);
		// call function recursively to write header
		writeTree(tree.root(), out);
		return headerSize();
	}

	/**
	 * helper method to write tree to the header recursively
	 * 
	 * @param node
	 * @param out
	 */
	private void writeTree(IHuffBaseNode node, BitOutputStream out) {
		if (node.isLeaf()) {
			out.write(1, 1);
			// write 9 bit to file
			out.write(BITS_PER_WORD + 1, ((HuffLeafNode) node).element());
			// System.out.println(path);
			return;
		} else {
			IHuffBaseNode left = ((HuffInternalNode) node).left();
			IHuffBaseNode right = ((HuffInternalNode) node).right();
			// write 0 indicating internal node
			out.write(1, 0);
			// write left and right
			writeTree(left, out);
			writeTree(right, out);
			return;
		}
	}

	/**
	 * Read the header and return an ITreeMaker object corresponding to the
	 * information/header read.
	 * 
	 * @param in
	 *            is source of bits for header
	 * @return an ITreeMaker object representing the tree stored in the header
	 * @throws IOException
	 *             if the header is bad, e.g., wrong MAGIC_NUMBER, wrong number of
	 *             bits, I/O error occurs reading
	 */
	public HuffTree readHeader(BitInputStream in) throws IOException {
		int magic = in.read(BITS_PER_INT);
		if (magic != MAGIC_NUMBER) {
			throw new IOException("magic number not right");
		}
		IHuffBaseNode root = readHeaderHelper(in);
		// initialize the tree
		HuffTree tree = new HuffTree(-1, -1);
		tree.setRoot(root);
		return tree;
	}

	/**
	 * helper method to read header recursively for building tree
	 * 
	 * @param node
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public IHuffBaseNode readHeaderHelper(BitInputStream in) throws IOException {
		if ((in.read(1) & 1) == 1) {
			// construct a new leaf node, make it weight as -1
			IHuffBaseNode newLeaf = new HuffLeafNode(in.read(9), -1);
			return newLeaf;
		} else {
			// construct a new internal node, make it weight as -1
			IHuffBaseNode newInternal = new HuffInternalNode(null, null, -1);
			((HuffInternalNode) newInternal).setLeft(readHeaderHelper(in));
			((HuffInternalNode) newInternal).setRight(readHeaderHelper(in));
			return newInternal;
		}
	}

}
