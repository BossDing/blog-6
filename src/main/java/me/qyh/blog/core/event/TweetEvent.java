/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * CeventTypeyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a ceventTypey of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.core.event;

import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationEvent;

import me.qyh.blog.core.entity.Tweet;

/**
 * 
 * @author Administrator
 *
 */
public class TweetEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final List<Tweet> tweets;
	private final EventType eventType;

	/**
	 * 
	 * @param source
	 *            操作对象
	 * @param article
	 *            文章
	 * @param eventType
	 *            操作方式
	 */
	public TweetEvent(Object source, Tweet article, EventType eventType) {
		super(source);
		this.tweets = Collections.singletonList(article);
		this.eventType = eventType;
	}

	/**
	 * 
	 * @param source
	 *            操作对象
	 * @param tweets
	 *            文章集合
	 * @param eventType
	 *            操作方式
	 */
	public TweetEvent(Object source, List<Tweet> tweets, EventType eventType) {
		super(source);
		this.tweets = tweets;
		this.eventType = eventType;
	}

	public List<Tweet> getTweets() {
		return tweets;
	}

	public EventType getEventType() {
		return eventType;
	}

}
