package com.github.shokohara.seed

import munit.FunSuite

class MySuite extends FunSuite {
  test("hello") {
    val obtained = 42
    val expected = 43
    assertEquals(obtained, expected)
  }
}
