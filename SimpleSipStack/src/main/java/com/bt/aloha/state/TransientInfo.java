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

 	

 	
 	
 
package com.bt.aloha.state;

import java.util.Map;

public class TransientInfo extends StateInfoBase<TransientInfo> {
    private static final long serialVersionUID = 9218359345815703051L;
    private Map<String, Object> transientsMap;

    public TransientInfo(String id, Map<String, Object> theTransientMap) {
    	//TODO: Figure out why Transient extends StateInfoBase
    	super(null);
        setId(id);
        this.transientsMap = theTransientMap;
    }

    @Override
    public TransientInfo cloneObject() {
        return (TransientInfo)super.cloneObject();
    }

    @Override
    public boolean isDead() {
        // TODO: MEDIUM: work out when these are 'dead'
        return false;
    }

    /**
     * TODO:  MEDIUM: Explain what's going on here?!  2 methods that do nothing, and 2 methods that sound very similar that do something
     */

    @Override
    public Map<String, Object> getTransients() {
        return null;
    }

    @Override
    public void setTransients(Map<String, Object> m) {
    }

    public Map<String, Object> getTransientsMap() {
        return transientsMap;
    }

    public void setTransientMap(Map<String, Object> m) {
        this.transientsMap = m;
    }
}
