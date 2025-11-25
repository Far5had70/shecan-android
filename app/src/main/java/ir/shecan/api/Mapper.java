package ir.shecan.api;

public interface Mapper<F, T> {
    T map(F input);
}