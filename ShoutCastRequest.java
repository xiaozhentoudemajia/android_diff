package com.aca.tunesremote.util;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by jim.yu on 2017/9/22.
 */

public class ShoutCastRequest {
    private String LogTag = ShoutCastRequest.class.getName();
    private String DeviceID = "hUNsgQY47tdTYkNp";
    private final int SHOUTCAST_RESPONSE_XML = 0;
    private final int SHOUTCAST_RESPONSE_JSON = 1;
    private final int SHOUTCAST_RESPONSE_M3U = 2;

    private final int eSHOUTCAST_XML_STATION = 0;
    private final int eSHOUTCAST_XML_GENRE = 1;
    private final int eSHOUTCAST_XML_RESPONSE = 2;

    private XmlPullParserFactory factory;
    private XmlPullParser parser;

    public class radioStation{
        String name;
        String mediaType;
        int id;
        int bitrate;
        List<String> genre = new ArrayList<String>();
        String ct;
        int lc;
        String logo;

        public void setName(String m_name){
            name = m_name;
        }

        public void setMediaType(String m_type){
            mediaType = m_type;
        }

        public void setId(int m_id){
            id = m_id;
        }

        public void setBitrate(int m_bitrate){
            bitrate = m_bitrate;
        }

        public void setGenre(String m_genre){
            genre.add(m_genre);
        }

        public void setCt(String m_ct){
            ct = m_ct;
        }

        public void setLc(int m_lc){
            lc =m_lc;
        }

        public void setLogo(String m_logo){
            logo = m_logo;
        }
    }

    public class radioGenre{
        String name;
        int id;
        int parent_id;
        boolean hasChildren;
        ArrayList<radioGenre> children = new ArrayList<radioGenre>();

        public void setName(String m_name){
            name = m_name;
        }
        public String getName() {
            return name;
        }

        public void setId(int m_id){
            id = m_id;
        }
        public int getId() {
            return id;
        }

        public void setParent_id(int m_parentId) {
            parent_id = m_parentId;
        }
        public int getParent_id() {
            return parent_id;
        }

        public void setHasChildren(boolean m_hasChildren) {
            hasChildren = m_hasChildren;
        }
        public boolean getHasChildren() {
            return hasChildren;
        }

        public void setChildren(radioGenre m_children) {
            children.add(m_children);
        }
    }

    private ArrayList<radioStation> stationList = new ArrayList<radioStation>();
    private ArrayList<radioGenre> genreList = new ArrayList<radioGenre>();

    //Process data return by ShoutCast server
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOUTCAST_RESPONSE_XML:
                    String response = (String) msg.obj;
                    Log.e(LogTag, "Response is :::" + response);
                    try {
                        factory = XmlPullParserFactory.newInstance();
                        parser = factory.newPullParser();
                        parser.setInput(new ByteArrayInputStream(response.getBytes()), "UTF-8");
                        boolean b_firstTag = true;
                        int xmlType = -1;
                        boolean b_needClearGenreList = true;
                        boolean b_rootGenre = true;
                        String tag;
                        String attributeName;
                        radioStation rs = null;
                        radioGenre rg = null;
//                        builder = factory.newDocumentBuilder();
//                        Document doc = builder.parse(new ByteArrayInputStream(response.getBytes()));

//                        NodeList nList = doc.getElementsByTagName("tunein");

                        int eventType = parser.getEventType();
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            switch (eventType) {
                                case XmlPullParser.START_DOCUMENT:
                                    break;
                                case XmlPullParser.START_TAG:
                                    tag = parser.getName();
                                    if (tag.equals("stationlist")) {
                                        //station list
                                        stationList.clear();
                                    } else if (tag.equals("station")) {
                                        //station
                                        rs = new radioStation();
                                        int num = parser.getAttributeCount();
                                        for(int i = 0; i < num; i++){
                                            attributeName = parser.getAttributeName(i);
                                            if (attributeName.equals("name")) {
                                                rs.setName(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("mt")) {
                                                rs.setMediaType(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("id")) {
                                                rs.setId(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("br")) {
                                                rs.setBitrate(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("ct")) {
                                                rs.setCt(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("lc")) {
                                                rs.setLc(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.startsWith("genre")) {
                                                rs.setGenre(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("logo")){
                                                rs.setLogo(parser.getAttributeValue(i));
                                            } else {
                                                Log.e("XML", "xml station attribute contain unknow tag:" + attributeName);
                                            }
                                        }
                                    } else if (tag.equals("tunein")) {
                                        //tunein m3u/pls
                                    } else if (tag.equals("genrelist")) {
                                        //genre list
                                        if (b_needClearGenreList) {
                                            b_needClearGenreList = false;
                                            genreList.clear();
                                        } else {
                                            Log.e("XML", "Sub genre list");
                                        }
                                    } else if (tag.equals("genre")) {
                                        rg = new radioGenre();
                                        int num = parser.getAttributeCount();
                                        for (int i = 0; i < num; i++) {
                                            attributeName = parser.getAttributeName(i);
                                            if (attributeName.equals("name")) {
                                                rg.setName(parser.getAttributeValue(i));
                                            } else if (attributeName.equals("id")) {
                                                rg.setId(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("parentid")) {
                                                rg.setParent_id(Integer.valueOf(parser.getAttributeValue(i)));
                                            } else if (attributeName.equals("haschildren")) {
                                                rg.setHasChildren(Boolean.valueOf(parser.getAttributeValue(i)));
                                            } else {
                                                Log.e("XML", "xml genre attribute contain unknow tag:" + attributeName);
                                            }
                                        }
                                    } else if (tag.equals("statusCode")) {
                                        Log.e("XML", "xml response code " + parser.nextText());
                                    } else if (tag.equals("statusText")) {
                                        Log.e("XML", "xml response text " + parser.nextText());
                                    } else {
                                        Log.e("XML", "xml genre attribute contain unknow tag:" + tag);
                                    }
//                                    if(b_firstTag){
//                                        b_firstTag = false;
//                                        if (0 == tag.compareTo("stationlist")) {
//                                            //station list xml
//                                            xmlType = eSHOUTCAST_XML_STATION;
//                                            stationList = new ArrayList<radioStation>();
//                                        } else if (0 == tag.compareTo("response")) {
//                                            //xml with response code
//                                            xmlType = eSHOUTCAST_XML_RESPONSE;
//                                        } else if (0 == tag.compareTo("genrelist")) {
//                                            //genre list xml
//                                            xmlType = eSHOUTCAST_XML_GENRE;
//                                            genreList = new ArrayList<radioGenre>();
//                                        } else {
//                                            xmlType = -1;
//                                        }
//                                    } else {
//                                        if (eSHOUTCAST_XML_STATION == xmlType) {
//                                            //station list
//                                            if (tag.equals("station")) {
//                                                rs = new radioStation();
//                                                int num = parser.getAttributeCount();
//                                                for(int i = 0; i < num; i++){
//                                                    if (parser.getAttributeName(i).equals("name")) {
//                                                        rs.setName(parser.getAttributeValue(i));
//                                                    } else if (parser.getAttributeName(i).equals("mt")) {
//                                                        rs.setMediaType(parser.getAttributeValue(i));
//                                                    } else if (parser.getAttributeName(i).equals("id")) {
//                                                        rs.setId(Integer.valueOf(parser.getAttributeValue(i)));
//                                                    } else if (parser.getAttributeName(i).equals("br")) {
//                                                        rs.setBitrate(Integer.valueOf(parser.getAttributeValue(i)));
//                                                    } else if (parser.getAttributeName(i).equals("ct")) {
//                                                        rs.setCt(parser.getAttributeValue(i));
//                                                    } else if (parser.getAttributeName(i).equals("lc")) {
//                                                        rs.setLc(Integer.valueOf(parser.getAttributeValue(i)));
//                                                    } else if (parser.getAttributeName(i).startsWith("genre")) {
//                                                        rs.setGenre(parser.getAttributeValue(i));
//                                                    } else if (parser.getAttributeName(i).equals("logo")){
//                                                        rs.setLogo(parser.getAttributeValue(i));
//                                                    } else {
//                                                        Log.e("XML", "xml station attribute contain unknow tag:" + parser.getAttributeName(i));
//                                                    }
//                                                }
//                                            }
//                                        } else if (eSHOUTCAST_XML_GENRE == xmlType) {
//                                            //genre list
//                                            if (tag.equals("genre")) {
//                                                rg = new radioGenre();
//                                                int num = parser.getAttributeCount();
//                                                for (int i = 0; i < num; i++) {
//                                                    if (parser.getAttributeName(i).equals("name")) {
//                                                        rg.setName(parser.getAttributeValue(i));
//                                                    } else if (parser.getAttributeName(i).equals("id")) {
//                                                        rg.setId(Integer.valueOf(parser.getAttributeValue(i)));
//                                                    } else if (parser.getAttributeName(i).equals("parentid")) {
//                                                        rg.setParent_id(Integer.valueOf(parser.getAttributeValue(i)));
//                                                    } else if (parser.getAttributeName(i).equals("haschildren")) {
//                                                        rg.setHasChildren(Boolean.valueOf(parser.getAttributeValue(i)));
//                                                    } else {
//                                                        Log.e("XML", "xml genre attribute contain unknow tag:" + parser.getAttributeName(i));
//                                                    }
//                                                }
//                                            }
//                                        } else if (eSHOUTCAST_XML_RESPONSE == xmlType) {
//                                            if (tag.equals("statusCode")) {
////                                                parser.nextText();
//                                            } else if (tag.equals("statusText")) {
//
//                                            } else if (tag.equals("genrelist")) {
//
//                                            } else if (tag.equals("genre")) {
//                                                rg = new radioGenre();
//                                                int num = parser.getAttributeCount();
//                                                for (int i = 0; i < num; i++) {
//                                                    if (parser.getAttributeName(i).equals("name")) {
//                                                        rg.setName(parser.getAttributeValue(i));
//                                                    } else if (parser.getAttributeName(i).equals("id")) {
//                                                        rg.setId(Integer.valueOf(parser.getAttributeValue(i)));
//                                                    } else if (parser.getAttributeName(i).equals("parentid")) {
//                                                        rg.setParent_id(Integer.valueOf(parser.getAttributeValue(i)));
//                                                    } else if (parser.getAttributeName(i).equals("haschildren")) {
//                                                        rg.setHasChildren(Boolean.valueOf(parser.getAttributeValue(i)));
//                                                    } else {
//                                                        Log.e("XML", "xml genre attribute contain unknow tag:" + parser.getAttributeName(i));
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
                                    break;
                                case XmlPullParser.END_TAG:
                                    tag = parser.getName();
                                    if (tag.equals("stationlist")) {

                                    } else if (tag.equals("station")) {
                                        stationList.add(rs);
                                        rs = null;
                                    } else if (tag.equals("genre")) {
                                        if (0 == rg.getParent_id()) {
                                            //primary genre
                                            genreList.add(rg);
                                        } else {
                                            //secondary genre, need add to parent genre
                                            radioGenre pg = genreList.get(genreList.size() - 1);
                                            if (pg.getId() == rg.getParent_id()) {

                                            } else {
                                                //need find parent genre
                                                Log.e("XML", "Parent genre is not " + (genreList.size() - 1));
                                                for (int i = 0; i < genreList.size(); i++) {
                                                    if (genreList.get(i).getId() == rg.getParent_id()) {
                                                        pg = genreList.get(i);
                                                        Log.e("XML", "Parent genre is " + i);
                                                        break;
                                                    }
                                                }
                                            }
                                            pg.setChildren(rg);
                                        }
                                        rg = null;
                                    } else if (tag.equals("genrelist")) {

                                    }
                                    break;
                            }
                            eventType = parser.next();
                        }

//                        NodeList nList = doc.getElementsByTagName("station");
//                        stationList.clear();
//                        for(int i = 0; i < nList.getLength(); i++){
//                            Element stationElement = (Element) nList.item(i);
//                            radioStation rs = new radioStation();
//                            rs.setName(stationElement.getAttribute("name"));
//                            rs.setMediaType(stationElement.getAttribute("mt"));
//                            rs.setId(Integer.valueOf(stationElement.getAttribute("id")));
//                            rs.setBitrate(Integer.valueOf(stationElement.getAttribute("br")));
//                            rs.setGenre(stationElement.getAttribute("genre"));
//                            rs.setCt(stationElement.getAttribute("ct"));
//                            rs.setLc(Integer.valueOf(stationElement.getAttribute("lc")));
//
//                            stationList.add(rs);
//                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                case SHOUTCAST_RESPONSE_M3U:

            }
        }
    };

    //Send request to ShoutCast server and get server response
    private void sendRequestWithHttpClient(final String url, final int responseType){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);

                try {
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    if(200 == httpResponse.getStatusLine().getStatusCode()){
                        HttpEntity entity = httpResponse.getEntity();
                        String response = EntityUtils.toString(entity,"utf-8");

                        Message message = Message.obtain();
                        message.what = responseType;
                        message.obj = response;
                        handler.sendMessage(message);
                    } else {
                        Log.e(LogTag, "Get method failed!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //get top 500 station
    public int getTop500Stations(int limit, int bitrate, String mediaType){

        String requestStr = "http://api.shoutcast.com/legacy/Top500?k=" + DeviceID;

        if(limit > 0 && limit <501)
            requestStr += "&limit=" + Integer.toString(limit);
        else if (limit < 0 || limit > 500)
            Log.e(LogTag, "Param limit error in getTop500Station func!");

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getTop500Station func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getTop500Station func!");
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by keyword search
    public int getStationsByKeyword(int offset, int limit, int bitrate, String mediaType, String searchStr){
        String requestStr = "http://api.shoutcast.com/legacy/stationsearch?k" + DeviceID;

        if(searchStr != null && !searchStr.isEmpty()){
            requestStr += "&search=" + searchStr;
        } else {
            Log.e(LogTag, "Param searchStr error in getStationsByKeyword func!");
            return -1;
        }

        if(-1 == offset){//no offset
            if(limit > 0){
                requestStr += "&limit=" + Integer.toString(limit);
            } else if (limit < 0){
                Log.e(LogTag, "Param limit error in getStationsByKeyword func!");
            }
        } else if(offset >= 0) {
            if(limit > 0){
                requestStr += "&limit=" + Integer.toString(offset) + "," + Integer.toString(limit);
            } else {
                Log.e(LogTag, "Param limit error in getStationsByKeyword func!");
            }
        } else {
            Log.e(LogTag, "Param offset error in getStationsByKeyword func!");
        }

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getStationsByKeyword func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getStationsByKeyword func!");
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by Genre
    public int getStationsByGenre(int offset, int limit, int bitrate, String mediaType, String genreStr){
        String requestStr = "http://api.shoutcast.com/legacy/genresearch?k=" + DeviceID;

        if(genreStr != null && !genreStr.isEmpty()){
            requestStr += "&genre=" + genreStr;
        } else {
            Log.e(LogTag, "Param genreStr error in getStationsByGenre func!");
            return -1;
        }

        if(-1 == offset){//no offset
            if(limit > 0){
                requestStr += "&limit=" + Integer.toString(limit);
            } else if (limit < 0){
                Log.e(LogTag, "Param limit error in getStationsByKeyword func!");
            }
        } else if(offset >= 0) {
            if(limit > 0){
                requestStr += "&limit=" + Integer.toString(offset) + "," + Integer.toString(limit);
            } else {
                Log.e(LogTag, "Param limit error in getStationsByKeyword func!");
            }
        } else {
            Log.e(LogTag, "Param offset error in getStationsByKeyword func!");
        }

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getStationsByKeyword func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getStationsByKeyword func!");
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by Now playing
    public int getStationsBaseOnNowPlayingInfo(int limit, int bitrate, String mediaType, String  genre, String ct){
        String requestStr = "http://api.shoutcast.com/station/nowplaying?k=" + DeviceID;

        if(ct != null && !ct.isEmpty()){
            requestStr += "&ct=" + ct;
        } else {
            Log.e(LogTag, "Param genreStr error in getStationsBaseOnNowPlayingInfo func!");
            return -1;
        }

        requestStr += "&f=xml";//default response xml

        if(limit > 0){
            requestStr += "&limit=" + Integer.toString(limit);
        } else if(limit < 0){
            Log.e(LogTag, "Param limit error in getStationsBaseOnNowPlayingInfo func!");
        }

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getStationsBaseOnNowPlayingInfo func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getStationsBaseOnNowPlayingInfo func!");
        }

        if(genre != null && !genre.isEmpty()){
            requestStr += "&genre=" + genre;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by GenreID
    public int getStationsByBitrateOrCodecTypeOrGenreID(int bitrate, String mediaType, int genreID, int limit, String genre){
        String requestStr = "http://api.shoutcast.com/station/advancedsearch?k=" + DeviceID + "&f=xml";

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getStationsByBitrateOrCodecTypeOrGenreID func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getStationsByBitrateOrCodecTypeOrGenreID func!");
        }

        if(genreID > 0)
            requestStr += "&genre_id=" + Integer.toString(genreID);
        else if (genreID < 0)
            Log.e(LogTag, "Param genreID error in getStationsByBitrateOrCodecTypeOrGenreID func!");

        if(limit > 0){
            requestStr += "&limit=" + Integer.toString(limit);
        } else if(limit < 0){
            Log.e(LogTag, "Param limit error in getStationsByBitrateOrCodecTypeOrGenreID func!");
        }

        if(genre != null && !genre.isEmpty()){
            requestStr += "&genre=" + genre;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //get station by random
    public int getRandomStations(int limit, int bitrate, String mediaType, String  genre){
        String requestStr = "http://api.shoutcast.com/station/randomstations?k=" + DeviceID + "&f=xml";

        if(limit > 0){
            requestStr += "&limit=" + Integer.toString(limit);
        } else if(limit < 0){
            Log.e(LogTag, "Param limit error in getRandomStations func!");
        }

        if(bitrate > 0)
            requestStr += "&br=" + Integer.toString(bitrate);
        else if (bitrate < 0)
            Log.e(LogTag, "Param bitrate error in getRandomStations func!");

        if(mediaType != null && !mediaType.isEmpty()) {
            if (mediaType.equalsIgnoreCase("mp3"))
                requestStr += "&mt=audio/mpeg";
            else if (mediaType.equalsIgnoreCase("aac+"))
                requestStr += "&mt=audio/aacp";
            else
                Log.e(LogTag, "Param mediaType error in getRandomStations func!");
        }

        if(genre != null && !genre.isEmpty()){
            requestStr += "&genre=" + genre;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getAllGenre(){
        String requestStr = "http://api.shoutcast.com/legacy/genrelist?k=" + DeviceID;

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getPrimaryGenre(){
        String requestStr = "http://api.shoutcast.com/genre/primary?k=" + DeviceID + "&f=xml";

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getSecondGenre(int parentID){
        String requestStr = "http://api.shoutcast.com/genre/secondary?k=" + DeviceID + "&f=xml";

        if(parentID >= 0){
            requestStr += "&parentid=" + Integer.toString(parentID);
        } else {
            Log.e(LogTag, "Param parentID error in getSecondGenre func!");
            return -1;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getGenreDetailByGenreID(int genreID){
        String requestStr = "http://api.shoutcast.com/genre/secondary?k=" + DeviceID + "&f=xml";

        if(genreID >= 0){
            requestStr += "&id=" + Integer.toString(genreID);
        } else {
            Log.e(LogTag, "Param genreID error in getGenreDetailByGenreID func!");
            return -1;
        }

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    public int getGenreBaseOnAvialabilitySubGenres(boolean hasChildren){
        String requestStr = "http://api.shoutcast.com/genre/secondary?k=" + DeviceID + "&f=xml";

        requestStr += "&haschildren=" + (hasChildren?"true":"false");

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_XML);
        return 0;
    }

    //tune in to station
    public void tuneIntoStation(String base, int stationID){
        String requestStr = "http://yp.shoutcast.com";
        if(null == base || base.isEmpty() || stationID < 0){
            Log.e(LogTag, "Param error in tuneIntoStation Func");
        }
        if(base.startsWith("/")){
            requestStr += base + "?";
        } else {
            requestStr += "/" + base + "?";
        }
        requestStr += "id=" + stationID;

        Log.i(LogTag, "Request String is " + requestStr);

        sendRequestWithHttpClient(requestStr, SHOUTCAST_RESPONSE_M3U);
    }
}
