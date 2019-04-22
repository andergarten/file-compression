import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;
import org.junit.Test;

public class HuffTest implements IHuffConstants {

	@Test
	public void testMakeHuffTree() throws IOException {
		Huff makeTree = new Huff();
		InputStream ins = new ByteArrayInputStream("teststr".getBytes("UTF-8"));
		HuffTree tree = makeTree.makeHuffTree(ins);
		assertEquals(tree.weight(), 8);
		ins.close();
	}

	@Test
	public void testMakeHuffTreeWhenNull() throws IOException {
		Huff makeTree = new Huff();
		InputStream ins = new ByteArrayInputStream("".getBytes("UTF-8"));
		HuffTree tree = makeTree.makeHuffTree(ins);
		assertEquals(tree.weight(), 1);
		ins.close();
	}

	@Test
	public void testMakeHuffTreeWhenOne() throws IOException {
		Huff makeTree = new Huff();
		InputStream ins = new ByteArrayInputStream(" ".getBytes("UTF-8"));
		HuffTree tree = makeTree.makeHuffTree(ins);
		assertEquals(tree.weight(), 2);
		ins.close();
	}

	@Test
	public void testMakeHuffTreeWhenMany() throws IOException {
		Huff makeTree = new Huff();
		InputStream ins = new ByteArrayInputStream("eeeeeeeeeetttoossssss".getBytes("UTF-8"));
		HuffTree tree = makeTree.makeHuffTree(ins);
		assertEquals(tree.weight(), 22);
		assertEquals(((HuffInternalNode) tree.root()).left().weight(), 10);
		ins.close();
	}

	@Test
	public void testShowCounts() throws IOException {
		Huff makeTree = new Huff();
		InputStream ins = new ByteArrayInputStream("teststr".getBytes("UTF-8"));
		makeTree.makeHuffTree(ins);
		assertEquals(makeTree.showCounts().get((int) 't'), new Integer(3));
		ins.close();
	}

	@Test
	public void testMakeTable() throws IOException {
		Huff makeTree = new Huff();
		InputStream ins = new ByteArrayInputStream("eeeeeeeeeetttoossssss".getBytes("UTF-8"));
		makeTree.makeHuffTree(ins);
		assertEquals(makeTree.makeTable().get((int) 't'), "110");
		ins.close();
	}

	@Test
	public void testGetCode() throws IOException {
		Huff makeTree = new Huff();
		InputStream ins = new ByteArrayInputStream("eeeeeeeeeetttoossssss".getBytes("UTF-8"));
		makeTree.makeHuffTree(ins);
		makeTree.makeTable();
		assertEquals(makeTree.getCode((int) 'e'), "0");
		ins.close();
	}

	@Test
	public void testWriteHeader() throws IOException {
		Huff writeHeader = new Huff();
		InputStream ins = new ByteArrayInputStream("t".getBytes("UTF-8"));
		writeHeader.makeHuffTree(ins);
		ins.close();
		// create the ByteArrayOutputStream
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		// construct a BitOutputStream from out
		// check the size of the header that was written
		assertEquals((BITS_PER_WORD + 1) * 2 + 3 + BITS_PER_INT, writeHeader.writeHeader(new BitOutputStream(out1)));
		// do not forget to close the stream
		out1.close();
	}

	@Test
	public void testHeaderSize() throws IOException {
		Huff writeHeader = new Huff();
		InputStream ins = new ByteArrayInputStream("trr".getBytes("UTF-8"));
		writeHeader.makeHuffTree(ins);
		ins.close();
		// create the ByteArrayOutputStream
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		// construct a BitOutputStream from out
		// check the size of the header that was written
		writeHeader.writeHeader(new BitOutputStream(out1));
		assertEquals((BITS_PER_WORD + 1) * 3 + 5 + BITS_PER_INT, writeHeader.headerSize());
		// do not forget to close the stream
		out1.close();
	}

	@Test
	public void TestReadHeaderHelper() throws IOException {
		Huff writeHeader = new Huff();
		BigInteger bi = new BigInteger("00100110000110001000001001110100", 2);
		byte[] bytes = bi.toByteArray();
		InputStream ins = new ByteArrayInputStream(bytes);
		IHuffBaseNode root = writeHeader.readHeaderHelper(new BitInputStream(ins));
		IHuffBaseNode right = ((HuffInternalNode) root).right();
		IHuffBaseNode left = ((HuffInternalNode) root).left();
		assertEquals(((HuffLeafNode) right).element(), 't');
		IHuffBaseNode leftleft = ((HuffInternalNode) left).left();
		IHuffBaseNode leftright = ((HuffInternalNode) left).right();
		assertEquals(((HuffLeafNode) leftleft).element(), 'a');
		assertEquals(((HuffLeafNode) leftright).element(), ' ');
		// do not forget to close the stream
		ins.close();
	}
	
	@Test (expected = IOException.class)
	public void TestReadHeaderWhenMagicException() throws IOException {
		Huff uncompress = new Huff();
		BigInteger bi = new BigInteger("00100110000110001000001001110100", 2);
		byte[] bytes = bi.toByteArray();
		InputStream ins = new ByteArrayInputStream(bytes);
		try {
			uncompress.readHeader(new BitInputStream(ins));
		}
		catch(IOException e) {
		      String message = "magic number not right";
		      assertEquals(message, e.getMessage());
		      ins.close();
		      throw e;
		}
	}

	@Test
	public void TestWriterWhenForce() {
		Huff write = new Huff();
		assertEquals(write.write("input.txt", "output.txt", true), 134);
	}
	
	@Test
	public void TestWriterWhenNotForce() {
		Huff write = new Huff();
		assertEquals(write.write("input.txt", "output.txt", false), 134);
	}
	
	@Test
	public void TestWriterWhenNotCompress() {
		Huff write = new Huff();
		assertEquals(write.write("input_1.txt", "output_1.txt", false), 0);
	}
	
	@Test
	public void TestUncompres() {
		Huff uncompress = new Huff();
		assertEquals(uncompress.uncompress("output.txt", "uncompressed.txt"), 168);
	}
	
	@Test
	public void TestUncompresWhenException() {
		Huff uncompress = new Huff();
		uncompress.uncompress("not_exist.txt", "not_exist_1.txt");
	}
	
	@Test (expected = IOException.class)
	public void TestReadCodeWhenException() throws Exception {
		Huff uncompress = new Huff();
		BigInteger bi = new BigInteger("00100110000110001000001001110100", 2);
		byte[] bytes = bi.toByteArray();
		// create input stream
		InputStream ins = new ByteArrayInputStream(bytes);
		// build tree
		uncompress.makeHuffTree(ins);
		// create the ByteArrayOutputStream
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IHuffBaseNode root = uncompress.readHeaderHelper(new BitInputStream(ins));
		HuffTree tree = new HuffTree(-1, -1);
		tree.setRoot(root);
		try {
			uncompress.readEncoding(tree, new BitInputStream(ins), new BitOutputStream(out));
		}
		catch(IOException e) {
		      String message = "unexpected end of input file";
		      assertEquals(message, e.getMessage());
		      ins.close();
		      out.close();
		      throw e;
		}
	}
}
