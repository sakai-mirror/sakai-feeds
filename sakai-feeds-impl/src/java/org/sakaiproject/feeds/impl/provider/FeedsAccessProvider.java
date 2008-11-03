package org.sakaiproject.feeds.impl.provider;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.IdEntityReference;
import org.sakaiproject.entitybroker.access.HttpServletAccessProvider;
import org.sakaiproject.entitybroker.access.HttpServletAccessProviderManager;
import org.sakaiproject.feeds.api.Feed;
import org.sakaiproject.feeds.api.FeedsService;
import org.sakaiproject.feeds.api.entity.ExternalFeedEntityProvider;
import org.sakaiproject.feeds.api.entity.InternalFeedEntityProvider;


public class FeedsAccessProvider implements HttpServletAccessProvider {
	private static Log							LOG	= LogFactory.getLog(FeedsAccessProvider.class);

	private FeedsService						m_feedsService;
	private HttpServletAccessProviderManager	m_httpServletAccessProviderManager;

	private List<String>						registeredPrefixes;

	// ######################################################
	// Spring methods
	// ######################################################
	public void setFeedsService(FeedsService feedsService) {
		this.m_feedsService = feedsService;
	}

	public void setHttpServletAccessProviderManager(HttpServletAccessProviderManager httpServletAccessProviderManager) {
		this.m_httpServletAccessProviderManager = httpServletAccessProviderManager;
	}

	public void init() {
		StringBuilder initMsg = new StringBuilder();
		registeredPrefixes = new ArrayList<String>();
		registeredPrefixes.add(InternalFeedEntityProvider.ENTITY_PREFIX);
		registeredPrefixes.add(ExternalFeedEntityProvider.ENTITY_PREFIX);
		for(String prefix : registeredPrefixes){
			m_httpServletAccessProviderManager.registerProvider(prefix, this);
			initMsg.append(" : "+prefix);
		}
		LOG.info("init(): Registered prefixes"+initMsg.toString()+" with "+this.getClass().getName());
	}

	public void destroy() {
		LOG.info("destroy()");
		for(String prefix : registeredPrefixes){
			m_httpServletAccessProviderManager.unregisterProvider(prefix, this);
			LOG.info("init(): Unregistered '"+prefix+"' from "+this.getClass().getName());
		}
	}

	// ######################################################
	// Service implementation methods
	// ######################################################
	/**
	 * Maps Feeds to http[s]://[sakai_server_name]/direct/[feed-id], where feed-id = /prefix/id <br/>
	 * Eg,
	 * http://sakaitest.ufp.pt:8080/direct/external-feed/ZmVlZDovL3d3dy5wdWJsaWNvLmNsaXgucHQvcnNzLmFzaHg=
	 */
	public void handleAccess(HttpServletRequest req, HttpServletResponse res, EntityReference ref) {
		if(ref instanceof IdEntityReference){
			try{
				// Prepare content
				Feed feed = m_feedsService.getFeed(ref, true);
				String feedXml = m_feedsService.getFeedXml(ref);

				// Content-Type
				String feedType = feed.getFeedType();
				String contentType = "application/rss+xml";
				if(feedType != null){
					if(feedType.startsWith("atom")){
						contentType = "application/atom+xml";
					}else if(feedType.startsWith("rss_0")){
						contentType = "text/xml";
					}
				}
				res.setContentType(contentType);

				// Character-Encoding
				String characterEncoding = feed.getFeedEncoding();
				if(characterEncoding == null || characterEncoding.equals(""))
					characterEncoding = "UTF-8";
				res.setCharacterEncoding(characterEncoding);

				// Content
				byte[] xmlBytes = feedXml.getBytes(characterEncoding);
				res.getOutputStream().write(xmlBytes);
			}catch(Exception e){
				LOG.error("Error occurred in FeedsAccessProvider.handleAccess()", e);
				return;
			}
		}else LOG.error("EntityReference " + ref + " is not a Feed.");
	}

}
