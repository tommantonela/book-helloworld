package modulo1exampleos;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

@RequestScoped
@Path("/geolocation") //Setea el path como URL de base + /geolocation
public class GeoLocationOS {

	// Los métodos para el pedido HTTP GET son definidos utilizando la anotación @GET  
	// La anotación @Produces define el tipo que retornará dicho método
		
	private HttpURLConnection openConnection(URL url){

		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		conn.setConnectTimeout(10000); 
		conn.setReadTimeout(10000);
		
		try {

			if(conn.getResponseCode()!=200){
				conn = null;
				
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return conn;
	}

	private String[] parseGoogleMapsContent(Object content) {

		String [] data = null;

		SAXBuilder builder = new SAXBuilder();
		Element rootNode = null;
		try {
			rootNode = ((Document)builder.build((InputStream)content)).getRootElement();
			if(rootNode.getChildText("status").equalsIgnoreCase("ok")){

				data = new String[4]; //city,latitude,longitud,country

				List<Element> list=rootNode.getChildren("result");
				Element i=list.get(0);
				Element l=i.getChild("geometry").getChild("location");

				List<Element> listAC=i.getChildren("address_component");

				String country = null;
				String locality = null;

				for(int j=0;j<listAC.size() && (country == null || locality == null);j++){

					List<Element> listTYPE = listAC.get(j).getChildren("type");

					for(int k=0;k<listTYPE.size() && (country==null || locality == null); k++){
						if(listTYPE.get(k).getText().equalsIgnoreCase("country"))
							country = listAC.get(j).getChildText("long_name");
						else
							if(listTYPE.get(k).getText().equalsIgnoreCase("locality"))
								locality = listAC.get(j).getChildText("long_name");
					}
				}

				data[0] = locality;
				data[1] = l.getChildText("lat");
				data[2] = l.getChildText("lng");
				data[3] = country;

			}	

		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}

		return data;
	}

	private String parseWikipediaInfo(Object content){
		String info = null;

		SAXBuilder builder = new SAXBuilder();
		Element rootNode = null;
		try {
			rootNode = ((Document)builder.build((InputStream)content)).getRootElement();

			Element query = rootNode.getChild("query");

			List<Element> pages = query.getChild("pages").getChildren("page");

			if(pages.size() > 0){
				Element firstPage = pages.get(0);

				info = firstPage.getChildText("extract");
			}
	
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		} 

		return info;
	}

	protected String getWikipediaInfo(String name){
		String summary = null;

		URL url = null;
		try {
			url = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=xml&exsentences=1&exintro=&explaintext=&exsectionformat=plain&titles="+URLEncoder.encode(name, "UTF-8"));
		} catch (MalformedURLException | UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		HttpURLConnection connection = openConnection(url);
		if(connection != null){
			try {
				summary = parseWikipediaInfo(connection.getContent());
				connection.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return summary;
	}

	protected String[] getGoogleMapsInfo(String place){
		
		String [] data = null;

		URL url = null;
		try {
			url = new URL("http://maps.googleapis.com/maps/api/geocode/xml?address="+URLEncoder.encode(place, "UTF-8"));//+"&key=AIzaSyAdW_DJG-x72U8ISQKVFlR4IGcAd8P4GsE");;
		} catch (MalformedURLException | UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		HttpURLConnection connection = openConnection(url);
		if(connection != null){

			try {
				data = parseGoogleMapsContent(connection.getContent());
				connection.disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return data;
	}

	@GET
	@Path("/processPlace")
	@Produces(MediaType.TEXT_XML)
	public String processPlace(@QueryParam("place") String place){
	
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\"?>");
		sb.append("<geolocation>");
		String [] data = getGoogleMapsInfo(place);
		
		if(data != null){
			
			sb.append("<city>"+data[0]+"</city>");
			sb.append(System.getProperty("line.separator"));
			sb.append("<country>"+data[3]+"</country>");
			sb.append(System.getProperty("line.separator"));
			sb.append("<latitude>"+data[1]+"</latitude>");
			sb.append(System.getProperty("line.separator"));
			sb.append("<longitude>"+data[2]+"</longitude>");
			sb.append(System.getProperty("line.separator"));
			sb.append("<info>");
			
			String wikiSummary = getWikipediaInfo(data[0]);
			if(wikiSummary != null)
				sb.append(wikiSummary);
			sb.append("</info>");
			sb.append(System.getProperty("line.separator"));
		}
		sb.append("</geolocation>");
		return sb.toString();
	}
}