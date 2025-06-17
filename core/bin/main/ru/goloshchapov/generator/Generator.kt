package ru.goloshchapov.generator

interface Generator<T> {
    fun generate(): T
}
