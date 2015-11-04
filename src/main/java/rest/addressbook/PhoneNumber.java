package rest.addressbook;

import static rest.addressbook.PhoneType.*;

/**
 * A phone number 
 *
 */
public class PhoneNumber {
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PhoneNumber other = (PhoneNumber) obj;
		if (number == null) {
			if (other.number != null) return false;
		}
		else if (!number.equals(other.number)) return false;
		if (type != other.type) return false;
		return true;
	}
	private String number;
	private PhoneType type = HOME;

	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public PhoneType getType() {
		return type;
	}
	public void setType(PhoneType type) {
		this.type = type;
	}

}
