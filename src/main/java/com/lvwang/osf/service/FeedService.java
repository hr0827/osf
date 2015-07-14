package com.lvwang.osf.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.lvwang.osf.dao.FeedDAO;
import com.lvwang.osf.model.Event;
import com.lvwang.osf.model.User;

@Service("feedService")
public class FeedService {

	public static final int FEED_COUNT_PER_PAGE = 3;
	
	@Autowired
	@Qualifier("followService")
	private FollowService followService;
	
	@Autowired
	@Qualifier("feedDao")
	private FeedDAO feedDao;
	
	@Autowired
	@Qualifier("eventService")
	private EventService eventService;
	
	@Autowired
	@Qualifier("userService")
	private UserService userService;
	
	@Autowired
	@Qualifier("likeService")
	private LikeService likeService;
	
	public void push(int user_id, int event_id) {
		List<Integer> followers = followService.getFollowerIDs(user_id);
		followers.add(user_id);	//add self
		if(followers != null && followers.size()!=0) {
			for(Integer follower: followers) {
				feedDao.save("feed:user:"+follower, event_id);
			}
		}
	}
	
	private List<Integer> getEventIDs(int user_id, int start, int count) {
		return feedDao.fetch("feed:user:"+user_id, start, count);
	}
	
	public List<Event> getFeeds(int user_id) {
		return getFeeds(user_id, FEED_COUNT_PER_PAGE);
	}
	
	public List<Event> getFeeds(int user_id, int count){
		List<Integer> event_ids = getEventIDs(user_id, 0, count-1);
		return decorateFeeds(user_id, event_ids);
	}
	
	private List<Event> decorateFeeds(int user_id, List<Integer> event_ids){
		List<Event> events = new ArrayList<Event>();
		if(event_ids != null && event_ids.size()!=0 ) {
			events = eventService.getEventsWithIDs(event_ids);
			addUserInfo(events);
			updLikeCount(user_id, events);
		}
		return events;
	}
	
	public List<Event> getFeedsOfPage(int user_id, int num) {
		List<Integer> event_ids = feedDao.fetch("feed:user:"+user_id, 
												FEED_COUNT_PER_PAGE*(num-1), 
												FEED_COUNT_PER_PAGE-1);
		return decorateFeeds(user_id, event_ids);
		
	}
	
	public void addUserInfo(List<Event> events) {
		if(events == null || events.size() == 0)
			return;
		for(Event event : events) {
			User user = userService.findById(event.getUser_id());
			event.setUser_name(user.getUser_name());
			event.setUser_avatar(user.getUser_avatar());
		}
	}
	
	public void updLikeCount(int user_id, List<Event> events){
		if(events == null || events.size() == 0)
			return;
		for(Event event : events) {
			event.setLike_count((int)likeService.likersCount(event.getObject_type(), 
															 event.getObject_id()));
			event.setIs_like(likeService.isLike(user_id, 
												event.getObject_type(), 
												event.getObject_id()));
		}
	}
	
	public void pull() {
		
	}
	
}
