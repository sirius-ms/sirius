
package de.unijena.bioinf.treealign.multijoin;

class TreeHashMap<T> {
    private final HashTable<T>[] tables;
    private final HashTable<T> emptyTable;
    private final int colSize;

    TreeHashMap(short maxJoins, int leftSize, int rightSize) {
        this.tables = new HashTable[leftSize * rightSize];
        this.colSize = leftSize;
        this.emptyTable = new HashTable<T>();
    }

    HashTable<T> get(int left, int right) {
        if (left < 0 || right < 0) return emptyTable;
        return this.tables[left + right * colSize];
    }

    void set(int left, int right, HashTable<T> table) {
        this.tables[left + right * colSize] = table;
    }
}
