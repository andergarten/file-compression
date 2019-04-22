import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CharCounter implements ICharCounter, IHuffConstants {

	private Map<Integer, Integer> count;
	
	public CharCounter() {
		count = new HashMap<Integer, Integer>();
	}

	 /**
     * Returns the count associated with specified character.
     * @param ch is the chunk/character for which count is requested
     * @return count of specified chunk
     * @throws the appropriate exception if ch isn't a valid chunk/character
     */
	public int getCount(int ch) {
		if (ch > 255) throw new IllegalArgumentException("illegal argument (> 255)");
		if (ch < 0) throw new IllegalArgumentException("illegal argument (< 0)");
		if (count.get(ch) == null) return 0;
		return count.get(ch);
	}

	/**
     * Initialize state by counting bits/chunks in a stream
     * @param stream is source of data
     * @return count of all chunks/read
     * @throws IOException if reading fails
     */
	public int countAll(InputStream stream) throws IOException {
		clear();
		// all chunks read
		int chunk = 0;
		// every chunk read
		int inbits;
		BitInputStream bits = new BitInputStream(stream);
		while ((inbits = bits.read(BITS_PER_WORD)) != -1) {
			// add this chunk to map
			add(inbits);
			chunk++;
		}
		// close bits
		bits.close();
		return chunk;
	}

	public void add(int i) {
		if (count.get(i) == null)
			set(i, 1);
		else
			set(i, count.get(i) + 1);
	}

	public void set(int i, int value) {
		count.put(i, value);
	}

	public void clear() {
		// All counts cleared to zero
		count.clear();
	}

	public Map<Integer, Integer> getTable() {
		return count;
	}

}
