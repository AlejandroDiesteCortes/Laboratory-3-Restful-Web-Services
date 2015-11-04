package rest.addressbook;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.After;
import org.junit.Test;

import rest.addressbook.AddressBook;
import rest.addressbook.ApplicationConfig;
import rest.addressbook.Person;

/**
 * A simple test suite
 *
 */
public class AddressBookServiceTest {

	HttpServer server;

	@Test
	public void serviceIsAlive() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		launchServer(ab);

		// Request the address book
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request().get();
		assertEquals(200, response.getStatus());

		// ////////////////////////////////////////////////////////////////////
		// Verify that GET /contacts is well implemented by the service, i.e
		// test that it is safe and idempotent
		// ////////////////////////////////////////////////////////////////////
		// we get the client list
		List<Person> list1 = response.readEntity(AddressBook.class)
				.getPersonList();
		assertEquals(0, list1.size());
		// we do another get
		response = client.target("http://localhost:8282/contacts").request()
				.get();
		assertEquals(200, response.getStatus());
		List<Person> list2 = response.readEntity(AddressBook.class)
				.getPersonList();
		// compare the lists
		assertEquals(list1, list2);

	}

	@Test
	public void createUser() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		launchServer(ab);

		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request().get();
		List<Person> list1 = response.readEntity(AddressBook.class)
				.getPersonList();

		// Prepare data
		Person juan = new Person();
		juan.setName("Juan");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/1");

		// Create a new user
		client = ClientBuilder.newClient();
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));

		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/1")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// ////////////////////////////////////////////////////////////////////
		// Verify that POST /contacts is well implemented by the service, i.e
		// test that it is not safe and not idempotent
		// ////////////////////////////////////////////////////////////////////

		// we check that it is not safe
		response = client.target("http://localhost:8282/contacts").request()
				.get();
		List<Person> list2 = response.readEntity(AddressBook.class)
				.getPersonList();
		assertNotEquals(list1, list2);
		// we check that it is not idempotent
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));
		juanUpdated = response.readEntity(Person.class);
		assertNotEquals(juanUpdated, 1);
		assertNotEquals(juanUpdated.getHref(), juanURI);
		response = client.target("http://localhost:8282/contacts").request()
				.get();
		List<Person> list3 = response.readEntity(AddressBook.class)
				.getPersonList();
		// compare the list before the las POST and the list after the last POST
		assertNotEquals(list2, list3);
	}

	@Test
	public void createUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(ab.nextId());
		ab.getPersonList().add(salvador);
		launchServer(ab);

		// Prepare data
		Person juan = new Person();
		juan.setName("Juan");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
		Person maria = new Person();
		maria.setName("Maria");
		URI mariaURI = URI.create("http://localhost:8282/contacts/person/3");

		// Create a user
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));
		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());

		// Create a second user
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(201, response.getStatus());
		assertEquals(mariaURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaUpdated.getName());
		assertEquals(3, mariaUpdated.getId());
		assertEquals(mariaURI, mariaUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaUpdated2 = response.readEntity(Person.class);
		assertEquals(mariaUpdated2.getName(), mariaUpdated.getName());
		assertEquals(mariaUpdated2.getId(), mariaUpdated.getId());
		assertEquals(mariaUpdated2.getHref(), mariaUpdated.getHref());

		// ////////////////////////////////////////////////////////////////////
		// Verify that GET /contacts/person/3 is well implemented by the
		// service, i.e
		// test that it is safe and idempotent
		// ////////////////////////////////////////////////////////////////////
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		// we compare that the name, id and ref
		// if these fields hasn't changed the action GET is safe, and idempotent
		mariaUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaUpdated.getName());
		assertEquals(3, mariaUpdated.getId());
		assertEquals(mariaURI, mariaUpdated.getHref());

	}

	@Test
	public void listUsers() throws IOException {

		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		Person juan = new Person();
		juan.setName("Juan");
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Test list of contacts
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		AddressBook addressBookRetrieved = response
				.readEntity(AddressBook.class);
		assertEquals(2, addressBookRetrieved.getPersonList().size());
		assertEquals(juan.getName(), addressBookRetrieved.getPersonList()
				.get(1).getName());

		// ////////////////////////////////////////////////////////////////////
		// Verify that POST is well implemented by the service, i.e
		// test that it is not safe and not idempotent
		// ////////////////////////////////////////////////////////////////////

	}

	@Test
	public void updateUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(ab.nextId());
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(ab.getNextId());
		URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Update Maria
		Person maria = new Person();
		maria.setName("Maria");
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), juanUpdated.getName());
		assertEquals(2, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Verify that the update is real
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaRetrieved = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaRetrieved.getName());
		assertEquals(2, mariaRetrieved.getId());
		assertEquals(juanURI, mariaRetrieved.getHref());

		// Verify that only can be updated existing values
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(400, response.getStatus());

		// ////////////////////////////////////////////////////////////////////
		// Verify that PUT /contacts/person/2 is well implemented by the
		// service, i.e
		// test that it is idempotent
		// ////////////////////////////////////////////////////////////////////

		// we verify that the state changed, so it isn't safe
		// only the name is changed
		assertNotEquals(juan.getName(), juanUpdated.getName());
		assertEquals(juan.getId(), juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());
		// we verify that PUT is idempotent
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		Person mariaUpdated = response.readEntity(Person.class);
		// name, id and href are the same as after first PUT
		// so PUT is idempotent
		assertEquals(mariaUpdated.getName(), juanUpdated.getName());
		assertEquals(mariaUpdated.getId(), juanUpdated.getId());
		assertEquals(mariaUpdated.getHref(), juanUpdated.getHref());
	}

	@Test
	public void deleteUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(1);
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(2);
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Delete a user
		Client client = ClientBuilder.newClient();
		// state before the first delete
		Response response = client.target("http://localhost:8282/contacts")
				.request().get();
		List<Person> list1 = response.readEntity(AddressBook.class)
				.getPersonList();

		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(204, response.getStatus());
		
		// state after the first delete and before the second delete
		response = client.target("http://localhost:8282/contacts").request()
				.get();
		List<Person> list2 = response.readEntity(AddressBook.class)
				.getPersonList();
		// Verify that the user has been deleted
		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(404, response.getStatus());

		// state after the second delete
		response = client.target("http://localhost:8282/contacts").request()
				.get();
		List<Person> list3 = response.readEntity(AddressBook.class)
				.getPersonList();
		// ////////////////////////////////////////////////////////////////////
		// Verify that DELETE /contacts/person/2 is well implemented by the
		// service, i.e
		// test that it is idempotent
		// ////////////////////////////////////////////////////////////////////

		//to verify that delete is idempotent we check that the state 
		//changes after the first delete but doesnt change after the second delete
		assertNotEquals(list1,list2);
		assertEquals(list2, list3);
	}

	@Test
	public void findUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(1);
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(2);
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Test user 1 exists
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/1")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person person = response.readEntity(Person.class);
		assertEquals(person.getName(), salvador.getName());
		assertEquals(person.getId(), salvador.getId());
		assertEquals(person.getHref(), salvador.getHref());

		// Test user 2 exists
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		person = response.readEntity(Person.class);
		assertEquals(person.getName(), juan.getName());
		assertEquals(2, juan.getId());
		assertEquals(person.getHref(), juan.getHref());

		// Test user 3 exists
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(404, response.getStatus());
	}

	private void launchServer(AddressBook ab) throws IOException {
		URI uri = UriBuilder.fromUri("http://localhost/").port(8282).build();
		server = GrizzlyHttpServerFactory.createHttpServer(uri,
				new ApplicationConfig(ab));
		server.start();
	}

	@After
	public void shutdown() {
		if (server != null) {
			server.shutdownNow();
		}
		server = null;
	}

}
