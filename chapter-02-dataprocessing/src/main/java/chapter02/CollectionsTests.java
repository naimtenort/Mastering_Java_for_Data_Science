package chapter02;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionsTests {

	public static void main(String[] args) {
		
		// 리스트
		List<String> list = new ArrayList<>();
		list.add("alpha");
		list.add("beta");
		list.add("beta");
		list.add("gamma");
		System.out.println(list);

		// 셋
		Set<String> set = new HashSet<>();
		set.add("alpha");
		set.add("beta");
		set.add("beta");
		set.add("gamma");
		System.out.println(set);

		// 셋 반복문 사용
		for (String el : set) {
		    System.out.println(el);
		}
		
		// 맵
		Map<String, String> map = new HashMap<>();
		map.put("alpha", "α");
		map.put("beta", "β");
		map.put("gamma", "γ");
		System.out.println(map);
		
		// 컬렉션 부가 기능
		String min = Collections.min(list);	// 최소값
		String max = Collections.max(list); // 최대값음
		System.out.println("min: " + min + ", max: " + max);
		Collections.sort(list); // 정렬
		Collections.shuffle(list); // 순서 섞음

	}

}
