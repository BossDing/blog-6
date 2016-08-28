package me.qyh.blog.lock;

import java.io.Serializable;

public interface LockKey extends Serializable {
	
	Object getKey();

}
