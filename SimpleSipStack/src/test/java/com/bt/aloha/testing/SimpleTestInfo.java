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

 	

 	
 	
 
package com.bt.aloha.testing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.bt.aloha.state.StateInfoBase;

public class SimpleTestInfo extends StateInfoBase<SimpleTestInfo> implements Serializable {
    private static final long serialVersionUID = -5439036194428490337L;

    private String f1;

    private transient String f2;

    public SimpleTestInfo(String id, String anf1, String anf2) {
        super(null);
        updateLastUsedTime();
        updateVersionId();
        this.setId(id);
        this.f1 = anf1;
        this.f2 = anf2;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    public String getF1() {
        return f1;
    }

    public String getF2() {
        return f2;
    }

    public void setF2(String s) {
        this.f2 = s;
    }

    @Override
    public Map<String, Object> getTransients() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("f2", f2);
        return result;
    }

    @Override
    public void setTransients(Map<String, Object> m) {
        if (m.containsKey("f2")) {
            this.f2 = (String)m.get("f2");
        }
    }
    
    public String toString(){
    	return String.format("[%s:%s,%s]", getId(), getF1(), getF2()); 
    }
}
