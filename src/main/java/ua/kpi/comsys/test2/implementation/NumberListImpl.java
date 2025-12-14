/*
 * Copyright (c) 2014, NTUU KPI, Computer systems department and/or its affiliates. All rights reserved.
 * NTUU KPI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 */
package ua.kpi.comsys.test2.implementation;

import ua.kpi.comsys.test2.NumberList;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Custom implementation of {@link NumberList}.
 *
 * <p>Represents a non-negative integer number. Each list element stores exactly one digit
 * in the selected numeral system (base) defined by the assignment.</p>
 *
 * <p>List type is selected by C3, numeral system by C5, additional base by (C5+1) mod 5,
 * additional operation by C7.</p>
 *
 * @author Andrii Savchenko, IO-35, record book 3517
 */
public class NumberListImpl implements NumberList {

    /** Student record book number (4 digits). */
    private static final int RECORD_BOOK_NUMBER = 3517;

    /** Primary base determined by C5. */
    private static final int PRIMARY_BASE = baseFromC5(RECORD_BOOK_NUMBER % 5);

    /** Additional base determined by (C5 + 1) mod 5. */
    private static final int ADDITIONAL_BASE = baseFromC5((RECORD_BOOK_NUMBER % 5 + 1) % 5);

    /** Additional operation selector (C7). */
    private static final int ADDITIONAL_OP = RECORD_BOOK_NUMBER % 7;

    /** List structure type selector (C3). */
    private static final int LIST_TYPE = RECORD_BOOK_NUMBER % 3;

    /** True if list is circular (ring). */
    private static final boolean CIRCULAR = (LIST_TYPE == 1 || LIST_TYPE == 2);

    /** True if list is doubly-linked. */
    private static final boolean DOUBLY = (LIST_TYPE == 0 || LIST_TYPE == 2);

    /** Node of the list. */
    private static final class Node {
        byte value;
        Node next;
        Node prev;

        Node(byte value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    /** Base for digits stored in this list. */
    private int base = PRIMARY_BASE;

    /** Decimal value of the number represented by this list. */
    private BigInteger decimalValue = BigInteger.ZERO;

    /** Default constructor. Creates an empty list (no digits). */
    public NumberListImpl() {
        this.base = PRIMARY_BASE;
        this.decimalValue = BigInteger.ZERO;
    }

    /**
     * Constructs list by a decimal number from a file (string format).
     * If file does not exist or contains invalid value, the list becomes empty.
     *
     * @param file file where a decimal number is stored
     */
    public NumberListImpl(File file) {
        this.base = PRIMARY_BASE;
        String s = readAllTrim(file);
        initFromDecimalString(s, PRIMARY_BASE);
    }

    /**
     * Constructs list by a decimal number in string notation.
     * If input is invalid, the list becomes empty.
     *
     * @param value decimal number in string notation
     */
    public NumberListImpl(String value) {
        this.base = PRIMARY_BASE;
        initFromDecimalString(value, PRIMARY_BASE);
    }

    private NumberListImpl(BigInteger decimal, int base) {
        this.base = base;
        this.decimalValue = (decimal == null) ? BigInteger.ZERO : decimal.max(BigInteger.ZERO);
        rebuildDigitsFromDecimal();
    }

    /**
     * Saves the number stored in the list into the specified file in decimal notation.
     *
     * @param file output file
     */
    public void saveList(File file) {
        if (file == null) return;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(toDecimalString());
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns student's record book number.
     *
     * @return record book number
     */
    public static int getRecordBookNumber() {
        return RECORD_BOOK_NUMBER;
    }

    /**
     * Returns a new list representing the same number in the additional base.
     * Does not modify the original list.
     *
     * @return number in the additional base
     */
    public NumberListImpl changeScale() {
        return new NumberListImpl(this.decimalValue, ADDITIONAL_BASE);
    }

    /**
     * Performs the additional operation defined by C7.
     * Operands remain unchanged.
     *
     * @param arg second operand
     * @return result as a new list in the primary base
     */
    public NumberListImpl additionalOperation(NumberList arg) {
        BigInteger a = this.decimalValue;
        BigInteger b = toBigInteger(arg);

        if (a.signum() < 0) a = BigInteger.ZERO;
        if (b.signum() < 0) b = BigInteger.ZERO;

        BigInteger r;
        switch (ADDITIONAL_OP) {
            case 0:
                r = a.add(b);
                break;
            case 1:
                r = a.subtract(b);
                if (r.signum() < 0) r = BigInteger.ZERO;
                break;
            case 2:
                r = a.multiply(b);
                break;
            case 3:
                r = b.equals(BigInteger.ZERO) ? BigInteger.ZERO : a.divide(b);
                break;
            case 4:
                r = b.equals(BigInteger.ZERO) ? BigInteger.ZERO : a.mod(b);
                break;
            case 5:
                r = a.and(b);
                break;
            case 6:
                r = a.or(b);
                break;
            default:
                r = BigInteger.ZERO;
                break;
        }
        return new NumberListImpl(r, PRIMARY_BASE);
    }

    /**
     * Returns decimal string representation of the number.
     *
     * @return decimal string
     */
    public String toDecimalString() {
        return decimalValue.toString();
    }

    @Override
    public String toString() {
        if (size == 0) return "";
        StringBuilder sb = new StringBuilder(size);
        Node n = head;
        for (int i = 0; i < size; i++) {
            sb.append(digitToChar(n.value & 0xFF));
            n = n.next;
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumberListImpl)) return false;
        NumberListImpl that = (NumberListImpl) o;
        return this.decimalValue.equals(that.decimalValue);
    }

    @Override
    public int hashCode() {
        return decimalValue.hashCode();
    }

    // ===== List API =====

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Byte)) return false;
        byte v = (Byte) o;
        Node n = head;
        for (int i = 0; i < size; i++) {
            if (n.value == v) return true;
            n = n.next;
        }
        return false;
    }

    @Override
    public Iterator<Byte> iterator() {
        return new Itr(0);
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        Node n = head;
        for (int i = 0; i < size; i++) {
            arr[i] = n.value;
            n = n.next;
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(Byte e) {
        if (e == null) throw new NullPointerException("Null digits are not allowed");
        ensureDigitInBase(e, base);
        linkLast(e);
        recalcDecimalFromDigits();
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Byte)) return false;
        byte v = (Byte) o;
        Node n = head;
        for (int i = 0; i < size; i++) {
            if (n.value == v) {
                unlink(n);
                recalcDecimalFromDigits();
                return true;
            }
            n = n.next;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null) return true;
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Byte> c) {
        if (c == null || c.isEmpty()) return false;
        boolean changed = false;
        for (Byte b : c) {
            add(b);
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Byte> c) {
        if (c == null || c.isEmpty()) return false;
        if (index < 0) index = 0;
        if (index > size) index = size;
        boolean changed = false;
        int i = index;
        for (Byte b : c) {
            add(i++, b);
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null || c.isEmpty()) return false;
        boolean changed = false;
        for (Object o : c) {
            while (remove(o)) changed = true;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            if (size == 0) return false;
            clear();
            return true;
        }

        boolean changed = false;
        Node n = head;
        int i = 0;
        while (i < size) {
            Node next = n.next;
            if (!c.contains(n.value)) {
                unlink(n);
                changed = true;
            } else {
                i++;
            }
            n = next;
        }
        if (changed) recalcDecimalFromDigits();
        return changed;
    }

    @Override
    public void clear() {
        head = tail = null;
        size = 0;
        decimalValue = BigInteger.ZERO;
    }

    @Override
    public Byte get(int index) {
        Node n = nodeAt(index);
        return (n == null) ? null : n.value;
    }

    @Override
    public Byte set(int index, Byte element) {
        if (element == null) throw new NullPointerException("Null digits are not allowed");
        ensureDigitInBase(element, base);
        Node n = nodeAt(index);
        if (n == null) return null;
        byte old = n.value;
        n.value = element;
        recalcDecimalFromDigits();
        return old;
    }

    @Override
    public void add(int index, Byte element) {
        if (element == null) throw new NullPointerException("Null digits are not allowed");
        ensureDigitInBase(element, base);

        if (index <= 0) {
            linkFirst(element);
        } else if (index >= size) {
            linkLast(element);
        } else {
            Node succ = nodeAt(index);
            if (succ == null) {
                linkLast(element);
            } else {
                if (DOUBLY) {
                    linkBefore(succ, element);
                } else {
                    Node pred = nodeAt(index - 1);
                    Node newNode = new Node(element);
                    newNode.next = succ;
                    pred.next = newNode;
                    size++;
                    normalizeLinks();
                }
            }
        }
        recalcDecimalFromDigits();
    }

    @Override
    public Byte remove(int index) {
        Node n = nodeAt(index);
        if (n == null) return null;
        byte old = n.value;
        unlink(n);
        recalcDecimalFromDigits();
        return old;
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte v = (Byte) o;
        Node n = head;
        for (int i = 0; i < size; i++) {
            if (n.value == v) return i;
            n = n.next;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Byte)) return -1;
        byte v = (Byte) o;
        int last = -1;
        Node n = head;
        for (int i = 0; i < size; i++) {
            if (n.value == v) last = i;
            n = n.next;
        }
        return last;
    }

    @Override
    public ListIterator<Byte> listIterator() {
        return new ListItr(0);
    }

    @Override
    public ListIterator<Byte> listIterator(int index) {
        return new ListItr(index);
    }

    @Override
    public List<Byte> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0) fromIndex = 0;
        if (toIndex > size) toIndex = size;
        if (toIndex < fromIndex) toIndex = fromIndex;

        NumberListImpl sub = new NumberListImpl();
        sub.base = this.base;

        Node n = nodeAt(fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            sub.linkLast(n.value);
            n = n.next;
        }
        sub.recalcDecimalFromDigits();
        return sub;
    }

    // ===== Additional operations from NumberList =====

    @Override
    public boolean swap(int index1, int index2) {
        if (index1 == index2) return true;
        if (index1 < 0 || index2 < 0 || index1 >= size || index2 >= size) return false;
        Node a = nodeAt(index1);
        Node b = nodeAt(index2);
        if (a == null || b == null) return false;
        byte tmp = a.value;
        a.value = b.value;
        b.value = tmp;
        recalcDecimalFromDigits();
        return true;
    }

    @Override
    public void sortAscending() {
        if (size < 2) return;
        int[] count = new int[base];
        Node n = head;
        for (int i = 0; i < size; i++) {
            int d = n.value & 0xFF;
            if (d >= 0 && d < base) count[d]++;
            n = n.next;
        }
        n = head;
        for (int d = 0; d < base; d++) {
            for (int k = 0; k < count[d]; k++) {
                n.value = (byte) d;
                n = n.next;
            }
        }
        recalcDecimalFromDigits();
    }

    @Override
    public void sortDescending() {
        if (size < 2) return;
        int[] count = new int[base];
        Node n = head;
        for (int i = 0; i < size; i++) {
            int d = n.value & 0xFF;
            if (d >= 0 && d < base) count[d]++;
            n = n.next;
        }
        n = head;
        for (int d = base - 1; d >= 0; d--) {
            for (int k = 0; k < count[d]; k++) {
                n.value = (byte) d;
                n = n.next;
            }
        }
        recalcDecimalFromDigits();
    }

    @Override
    public void shiftLeft() {
        if (size < 2) return;
        byte first = head.value;
        Node n = head;
        for (int i = 0; i < size - 1; i++) {
            n.value = n.next.value;
            n = n.next;
        }
        tail.value = first;
        recalcDecimalFromDigits();
    }

    @Override
    public void shiftRight() {
        if (size < 2) return;
        byte[] values = new byte[size];
        Node n = head;
        for (int i = 0; i < size; i++) {
            values[i] = n.value;
            n = n.next;
        }
        byte last = values[size - 1];
        for (int i = size - 1; i > 0; i--) {
            values[i] = values[i - 1];
        }
        values[0] = last;
        n = head;
        for (int i = 0; i < size; i++) {
            n.value = values[i];
            n = n.next;
        }
        recalcDecimalFromDigits();
    }

    // ===== Internals =====

    private void linkFirst(byte v) {
        Node newNode = new Node(v);
        if (size == 0) {
            head = tail = newNode;
            size = 1;
            normalizeLinks();
            return;
        }
        newNode.next = head;
        if (DOUBLY) {
            newNode.prev = CIRCULAR ? tail : null;
            head.prev = newNode;
        }
        head = newNode;
        size++;
        normalizeLinks();
    }

    private void linkLast(byte v) {
        Node newNode = new Node(v);
        if (size == 0) {
            head = tail = newNode;
            size = 1;
            normalizeLinks();
            return;
        }
        tail.next = newNode;
        if (DOUBLY) newNode.prev = tail;
        tail = newNode;
        size++;
        normalizeLinks();
    }

    private void linkBefore(Node succ, byte v) {
        Node newNode = new Node(v);
        Node pred = succ.prev;

        newNode.next = succ;
        newNode.prev = pred;
        succ.prev = newNode;

        if (pred != null) {
            pred.next = newNode;
        } else {
            head = newNode;
        }

        if (succ == head && CIRCULAR) {
            head = newNode;
        }

        size++;
        normalizeLinks();
    }

    private void unlink(Node x) {
        if (x == null || size == 0) return;
        if (size == 1) {
            head = tail = null;
            size = 0;
            return;
        }

        Node next = x.next;
        Node prev = DOUBLY ? x.prev : predecessorOf(x);

        if (x == head) {
            head = next;
        }
        if (x == tail) {
            tail = prev;
        }

        if (prev != null) {
            prev.next = next;
        }
        if (DOUBLY && next != null) {
            next.prev = prev;
        }

        size--;
        normalizeLinks();

        // detach
        x.next = null;
        x.prev = null;
    }

    private Node predecessorOf(Node target) {
        if (target == null || size == 0 || target == head) return null;
        Node prev = null;
        Node cur = head;
        for (int i = 0; i < size; i++) {
            if (cur == target) return prev;
            prev = cur;
            cur = cur.next;
        }
        return null;
    }

    private void normalizeLinks() {
        if (size == 0) {
            head = tail = null;
            return;
        }

        if (CIRCULAR) {
            tail.next = head;
            if (DOUBLY) head.prev = tail;
        } else {
            tail.next = null;
            if (DOUBLY) head.prev = null;
        }
    }

    private Node nodeAt(int index) {
        if (index < 0 || index >= size) return null;
        Node n = head;
        for (int i = 0; i < index; i++) {
            n = n.next;
        }
        return n;
    }

    private void initFromDecimalString(String s, int base) {
        String trimmed = (s == null) ? "" : s.trim();

        if (trimmed.isEmpty() || !trimmed.matches("\\d+")) {
            this.base = base;
            clear();
            return;
        }

        BigInteger v;
        try {
            v = new BigInteger(trimmed);
        } catch (Exception e) {
            this.base = base;
            clear();
            return;
        }

        if (v.signum() < 0) {
            this.base = base;
            clear();
            return;
        }

        this.base = base;
        this.decimalValue = v;
        rebuildDigitsFromDecimal();
    }

    private void rebuildDigitsFromDecimal() {
        clearNodesOnly();
        if (decimalValue == null) decimalValue = BigInteger.ZERO;

        if (decimalValue.equals(BigInteger.ZERO)) {
            linkLast((byte) 0);
            return;
        }

        BigInteger b = BigInteger.valueOf(base);
        BigInteger v = decimalValue;

        // collect digits in reverse order
        byte[] rev = new byte[Math.max(1, (v.bitLength() / 2) + 2)];
        int len = 0;
        while (v.signum() > 0) {
            BigInteger[] dr = v.divideAndRemainder(b);
            rev[len++] = (byte) dr[1].intValue();
            v = dr[0];
        }

        for (int i = len - 1; i >= 0; i--) {
            linkLast(rev[i]);
        }
    }

    private void clearNodesOnly() {
        head = tail = null;
        size = 0;
    }

    private void recalcDecimalFromDigits() {
        if (size == 0) {
            decimalValue = BigInteger.ZERO;
            return;
        }
        BigInteger b = BigInteger.valueOf(base);
        BigInteger v = BigInteger.ZERO;
        Node n = head;
        for (int i = 0; i < size; i++) {
            int d = n.value & 0xFF;
            v = v.multiply(b).add(BigInteger.valueOf(d));
            n = n.next;
        }
        decimalValue = v;
    }

    private static void ensureDigitInBase(byte digit, int base) {
        int d = digit & 0xFF;
        if (d < 0 || d >= base) {
            throw new IllegalArgumentException("Digit out of range for base " + base + ": " + d);
        }
    }

    private static char digitToChar(int d) {
        return (d < 10) ? (char) ('0' + d) : (char) ('A' + (d - 10));
    }

    private static int baseFromC5(int c5) {
        switch (c5) {
            case 0: return 2;
            case 1: return 3;
            case 2: return 8;
            case 3: return 10;
            case 4: return 16;
            default: return 10;
        }
    }

    private static BigInteger toBigInteger(NumberList list) {
        if (list == null) return BigInteger.ZERO;
        if (list instanceof NumberListImpl) {
            return ((NumberListImpl) list).decimalValue;
        }
        BigInteger b = BigInteger.valueOf(PRIMARY_BASE);
        BigInteger v = BigInteger.ZERO;
        for (Byte x : list) {
            if (x == null) return BigInteger.ZERO;
            int d = x & 0xFF;
            if (d < 0 || d >= PRIMARY_BASE) return BigInteger.ZERO;
            v = v.multiply(b).add(BigInteger.valueOf(d));
        }
        return v;
    }

    private static String readAllTrim(File file) {
        if (file == null) return "";

        if (file.exists() && file.isFile()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            } catch (IOException ignored) {
            }
            return sb.toString().trim();
        }

        String[] candidates = new String[] {
            file.getPath().replace('\\', '/'),
            file.getName()
        };

        for (String name : candidates) {
            try (InputStream is = NumberListImpl.class.getClassLoader().getResourceAsStream(name)) {
                if (is == null) continue;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                return sb.toString().trim();
            } catch (IOException ignored) {
            }
        }

        return "";
    }

    private class Itr implements Iterator<Byte> {
        int cursor;
        int lastRet = -1;

        Itr(int index) {
            this.cursor = Math.max(0, Math.min(index, size));
        }

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public Byte next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastRet = cursor;
            return get(cursor++);
        }

        @Override
        public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            NumberListImpl.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
        }
    }

    private class ListItr implements ListIterator<Byte> {
        int cursor;
        int lastRet = -1;

        ListItr(int index) {
            this.cursor = Math.max(0, Math.min(index, size));
        }

        @Override public boolean hasNext() { return cursor < size; }
        @Override public boolean hasPrevious() { return cursor > 0; }
        @Override public int nextIndex() { return cursor; }
        @Override public int previousIndex() { return cursor - 1; }

        @Override
        public Byte next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastRet = cursor;
            return get(cursor++);
        }

        @Override
        public Byte previous() {
            if (!hasPrevious()) throw new NoSuchElementException();
            cursor--;
            lastRet = cursor;
            return get(cursor);
        }

        @Override
        public void remove() {
            if (lastRet < 0) throw new IllegalStateException();
            NumberListImpl.this.remove(lastRet);
            if (lastRet < cursor) cursor--;
            lastRet = -1;
        }

        @Override
        public void set(Byte e) {
            if (lastRet < 0) throw new IllegalStateException();
            NumberListImpl.this.set(lastRet, e);
        }

        @Override
        public void add(Byte e) {
            NumberListImpl.this.add(cursor, e);
            cursor++;
            lastRet = -1;
        }
    }
}
