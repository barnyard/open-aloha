/*
 * Aloha Open Source SIP Application Server- https://trac.osmosoft.com/Aloha
 *
 * Copyright (c) 2008, British Telecommunications plc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * ClientAuthenticationMethod.java
 *
 * Created on January 8, 2003, 9:49 AM
 */

package examples.authorization;

/**
 * Get this interface from the nist-sip IM
 * @author  olivier deruelle
 */
public interface ClientAuthenticationMethod {
    
    /**
     * Initialize the Client authentication method. This has to be
     * done outside the constructor.
     * @throws Exception if the parameters are not correct.
     */
    public void initialize(String realm,String userName,String uri,String nonce
    ,String password,String method,String cnonce,String algorithm) throws Exception;
    
    
    /**
     * generate the response
     * @returns null if the parameters given in the initialization are not
     * correct.
     */
    public String generateResponse();
    
}
