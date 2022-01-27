package org.apache.http.main;

public class ProcessContext {

	private static ThreadLocal<Integer> poolMark = new ThreadLocal<>();

	private static ThreadLocal<String> beanName = new ThreadLocal<>();

	private static ThreadLocal<Boolean> sync = new ThreadLocal<>();

	public static void setPoolMark(int mark) {
		poolMark.set(mark);
	}

	public static int getPoolMark() {
		Integer integer = poolMark.get();
		if (integer == null) {
			return HttpClientUtils.DEFAULT_POOL;
		} else {
			return integer;
		}
	}

	public static void removePoolMark() {
		poolMark.remove();
	}

	public static void setBeanName(String value) {
		beanName.set(value);
	}

	public static String getBeanName() {
		return beanName.get();
	}

	public static void removeBeanName() {
		beanName.remove();
	}

	public static void setSync(boolean value) {
		sync.set(value);
	}

	public static Boolean getSync() {
		return sync.get();
	}

	public static void removeSync() {
		sync.remove();
	}

	public static void removeAll() {
		poolMark.remove();
		beanName.remove();
		sync.remove();
	}
}
