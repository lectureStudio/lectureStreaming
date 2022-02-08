package org.lecturestudio.web.portal.model.event;

public interface MessengerFeatureUserConnectionListener {

    public void onMessengerFeatureUserConnected(long courseId, String username);

    public void onMessengerFeatureUserDisconnected(long courseId, String username);
    
}
