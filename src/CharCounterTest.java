import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class CharCounterTest {

	@Test
	public void testGetCount() {
		ICharCounter cc = new CharCounter();
		cc.set('t', 3);
		assertEquals(cc.getCount('t'), 3);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetCountWhenNotValid() {
		ICharCounter cc = new CharCounter();
		try {
			cc.getCount(256);
		}
		catch (IllegalArgumentException e) {
			String m = "illegal argument (> 255)";
			assertEquals(m, e.getMessage());
		}
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetCountWhenNotValidLessThanZero() {
		ICharCounter cc = new CharCounter();
		try {
			cc.getCount(-1);
		}
		catch (IllegalArgumentException e) {
			String m = "illegal argument (< 0)";
			assertEquals(m, e.getMessage());
		}
	}
	
	@Test
	public void testGetCountWhenNoMapping() throws IOException {
		ICharCounter cc = new CharCounter();
		InputStream ins = new ByteArrayInputStream("teststr".getBytes("UTF-8"));
		cc.countAll(ins);
		assertEquals(cc.getCount('b'), 0);
	}

	@Test
	public void testCountAll() throws IOException {
		ICharCounter cc = new CharCounter();
		InputStream ins = new ByteArrayInputStream("teststr".getBytes("UTF-8"));
		cc.countAll(ins);
		assertEquals(cc.getCount('t'), 3);
		assertEquals(cc.getCount('s'), 2);
		assertEquals(cc.getCount('e'), 1);
		assertEquals(cc.getCount('r'), 1);
		ins.close();
	}
	
	@Test
	public void testCountAllWhenBlankSpace() throws IOException {
		ICharCounter cc = new CharCounter();
		InputStream ins = new ByteArrayInputStream(" ".getBytes("UTF-8"));
		cc.countAll(ins);
		assertEquals(cc.getCount(' '), 1);
		ins.close();
	}

	@Test
	public void testAdd() {
		ICharCounter cc = new CharCounter();
		cc.set('t', 3);
		cc.add('t');
		assertEquals(cc.getCount('t'), 4);
	}

	@Test
	public void testAddWhenZero() {
		ICharCounter cc = new CharCounter();
		cc.add('t');
		assertEquals(cc.getCount('t'), 1);
	}

	@Test
	public void testSet() {
		ICharCounter cc = new CharCounter();
		cc.set('t', 1);
		assertEquals(cc.getCount('t'), 1);
	}

	@Test
	public void testClear() {
		ICharCounter cc = new CharCounter();
		cc.add('t');
		cc.set('a', 100);
		cc.clear();
		assertEquals(cc.getCount('t'), 0);
		assertEquals(cc.getCount('a'), 0);
	}

	@Test
	public void testGetTable() {
		ICharCounter cc = new CharCounter();
		cc.add('t');
		cc.set('a', 100);
		Map<Integer, Integer> map = cc.getTable();
		assertEquals(map.get((int) 't'), new Integer(1));
		assertEquals(map.get((int) 'a'), new Integer(100));
	}

}
