package com.ban.cheonil.playground;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamApiTest {

    // @Test:  테스트 메서드 표시 어노테이션
    @Test
    void filter_map_collect() {
        List<Integer> nums = List.of(1, 2, 3, 4, 5, 6);

        List<Integer> result = nums.stream()
                .filter(n -> n % 2 == 0)
                .map(n -> n * 10)
                .collect(Collectors.toList());

        System.out.println("filter_map_collect = " + result);
        assertEquals(List.of(20, 40, 60), result);
    }

    @Test
    void filter_map_collect2() {
        List<Integer> nums = List.of(1,2,3,4,5,6,7);

        List<Integer> filters = nums.stream().filter(n -> n%2 == 0).toList();
        List<Integer> maps = nums.stream().map(n -> n * 10).toList();

        System.out.println("filters: " + filters);
        System.out.println("maps: " + maps);
    }

    @Test
    void reduce_sum() {
        int sum = IntStream.rangeClosed(1, 10).sum();
        int reduceSum = Stream.of(1, 2, 3, 4, 5).reduce(0, (a, b) -> a + b);

        System.out.println("1..10 합계 = " + sum);
        System.out.println("reduce 합계 = " + reduceSum);
        assertEquals(55, sum);
        assertEquals(15, reduceSum);
    }

    @Test
    void resuce_sum2() {
        int sum = IntStream.rangeClosed(1, 10).sum();
        int rduceSum = Stream.of(1,2,3,4,5).reduce(0, (a, b) -> a+b)    ;
    }

    @Test
    void groupingBy_example() {
        List<String> words = Arrays.asList("apple", "banana", "avocado", "blueberry", "cherry");

        Map<Character, List<String>> grouped = words.stream()
                .collect(Collectors.groupingBy(w -> w.charAt(0)));

        System.out.println("첫 글자별 그룹 = " + grouped);
        assertEquals(2, grouped.get('a').size());
        assertEquals(2, grouped.get('b').size());
        assertEquals(1, grouped.get('c').size());
    }

    @Test
    void sorted_distinct_limit() {
        List<Integer> result = Stream.of(5, 3, 1, 4, 1, 2, 5, 3)
                .distinct()
                .sorted()
                .limit(3)
                .toList();

        System.out.println("distinct+sorted+limit = " + result);
        assertEquals(List.of(1, 2, 3), result);
    }
}
