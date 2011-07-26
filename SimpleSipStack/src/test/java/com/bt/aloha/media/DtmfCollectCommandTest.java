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

 	

 	
 	
 
package com.bt.aloha.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.aloha.media.DtmfCollectCommand;
import com.bt.aloha.media.DtmfCollectCommandException;
import com.bt.aloha.media.DtmfLengthPattern;
import com.bt.aloha.media.DtmfMinMaxRetPattern;
import com.bt.aloha.media.DtmfPattern;

public class DtmfCollectCommandTest {

    @Test
    public void testDtmfCollectCommandFixedLength() {
        // setup
        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 44);

        // assert
        assertEquals("myUri", dtmfCollectCommand.getPromptFileUri());
        assertEquals(true, dtmfCollectCommand.isAllowBarge());
        assertEquals(true, dtmfCollectCommand.isClearBuffer());
        assertEquals(12, dtmfCollectCommand.getFirstDigitTimeoutSeconds());
        assertEquals(34, dtmfCollectCommand.getInterDigitTimeoutSeconds());
        assertEquals(56, dtmfCollectCommand.getExtraDigitTimeoutSeconds());
        assertEquals(44, ((DtmfLengthPattern) dtmfCollectCommand.getPattern()).getLength());
        assertEquals("length=44", ((DtmfLengthPattern) dtmfCollectCommand.getPattern()).toString());
        assertEquals("myUri;true;true;12;34;56;length=44", dtmfCollectCommand.toString());
    }

    @Test
    public void testDtmfCollectCommandFixedLengthAndCancel() {
        // setup
        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 44, '*');

        // assert
        assertEquals("myUri", dtmfCollectCommand.getPromptFileUri());
        assertEquals(true, dtmfCollectCommand.isAllowBarge());
        assertEquals(true, dtmfCollectCommand.isClearBuffer());
        assertEquals(12, dtmfCollectCommand.getFirstDigitTimeoutSeconds());
        assertEquals(34, dtmfCollectCommand.getInterDigitTimeoutSeconds());
        assertEquals(56, dtmfCollectCommand.getExtraDigitTimeoutSeconds());
        assertEquals(44, ((DtmfLengthPattern) dtmfCollectCommand.getPattern()).getLength());
        assertEquals('*', ((DtmfLengthPattern) dtmfCollectCommand.getPattern()).getCancelKey());
        assertEquals("length=44;cancel=*", ((DtmfLengthPattern) dtmfCollectCommand.getPattern()).toString());
        assertEquals("myUri;true;true;12;34;56;length=44;cancel=*", dtmfCollectCommand.toString());
    }

    @Test
    public void testDtmfCollectCommandMinMaxRet() {
        // setup
        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, 44, '#');

        // assert
        assertEquals("myUri", dtmfCollectCommand.getPromptFileUri());
        assertEquals(true, dtmfCollectCommand.isAllowBarge());
        assertEquals(true, dtmfCollectCommand.isClearBuffer());
        assertEquals(12, dtmfCollectCommand.getFirstDigitTimeoutSeconds());
        assertEquals(34, dtmfCollectCommand.getInterDigitTimeoutSeconds());
        assertEquals(56, dtmfCollectCommand.getExtraDigitTimeoutSeconds());
        assertEquals(33, ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getMinDigits());
        assertEquals(44, ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getMaxDigits());
        assertEquals('#', ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getReturnKey());
        assertEquals("min=33;max=44;rtk=#", ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).toString());
        assertEquals("myUri;true;true;12;34;56;min=33;max=44;rtk=#", dtmfCollectCommand.toString());
    }

    @Test
    public void testDtmfCollectCommandMinMaxRetCancel() {
        // setup
        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, 44, '#', '*');

        // assert
        assertEquals("myUri", dtmfCollectCommand.getPromptFileUri());
        assertEquals(true, dtmfCollectCommand.isAllowBarge());
        assertEquals(true, dtmfCollectCommand.isClearBuffer());
        assertEquals(12, dtmfCollectCommand.getFirstDigitTimeoutSeconds());
        assertEquals(34, dtmfCollectCommand.getInterDigitTimeoutSeconds());
        assertEquals(56, dtmfCollectCommand.getExtraDigitTimeoutSeconds());
        assertEquals(33, ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getMinDigits());
        assertEquals(44, ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getMaxDigits());
        assertEquals('#', ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getReturnKey());
        assertEquals('*', ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getCancelKey());
        assertEquals("min=33;max=44;rtk=#;cancel=*", ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).toString());
        assertEquals("myUri;true;true;12;34;56;min=33;max=44;rtk=#;cancel=*", dtmfCollectCommand.toString());
    }

    @Test
    public void testDtmfCollectCommandStringMinMaxOnly() {
        // setup
        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, 44);

        // assert
        assertEquals("myUri", dtmfCollectCommand.getPromptFileUri());
        assertEquals(true, dtmfCollectCommand.isAllowBarge());
        assertEquals(true, dtmfCollectCommand.isClearBuffer());
        assertEquals(12, dtmfCollectCommand.getFirstDigitTimeoutSeconds());
        assertEquals(34, dtmfCollectCommand.getInterDigitTimeoutSeconds());
        assertEquals(56, dtmfCollectCommand.getExtraDigitTimeoutSeconds());
        assertEquals(33, ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getMinDigits());
        assertEquals(44, ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getMaxDigits());
        assertEquals(null, ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).getReturnKey());
        assertEquals("min=33;max=44", ((DtmfMinMaxRetPattern) dtmfCollectCommand.getPattern()).toString());
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testNullMinDigits() {
        // setup/act/assert
        try {
        	new DtmfCollectCommand("myUri", true, true, 12, 34, 56, null, 44);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Min digits cannot be null") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testNullMaxDigits() {
        // setup/act/assert
        try {
        	new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, (Integer)null);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Max digits cannot be null") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidFirstDigitTimeoutSecondsLow() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, -1, 34, 56, 44);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("First digit timeout") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidFirstDigitTimeoutSecondsHigh() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 301, 34, 56, 44);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("First digit timeout") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidInterDigitTimeoutSecondsLow() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 0, 56, 44);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Inter digit timeout") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidInterDigitTimeoutSecondsHigh() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 301, 56, 44);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Inter digit timeout") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidExtraDigitTimeoutSecondsLow() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 0, 44);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Extra digit timeout") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidExtraDigitTimeoutSecondsHigh() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 301, 44);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Extra digit timeout") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidLengthLow() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 0);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Length must be") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidLengthHigh() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 51);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Length must be") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidMinDigitsLow() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 0, 44, '#');
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Min digits") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidMinDigitsHigh() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 51, 44, '#');
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Min digits") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidMaxDigitsLow() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, 0, '#');
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Max digits") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidMaxDigitsHigh() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, 51, '#');
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Max digits") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testMaxDigitsLessThanMaxDigits() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, 22, '#');
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("not be less than") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidReturnKey() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, 44, 'A');
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Return key must") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testInvalidCancelKey() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, 33, 44, '#', 'A');
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Cancel key must") > -1);
            throw e;
        }
    }

    @Test(expected = DtmfCollectCommandException.class)
    public void testMustSetAtLeastOneDtmfMinMaxRetPattern() {
        // setup/act/assert
        try {
            new DtmfCollectCommand("myUri", true, true, 12, 34, 56, -1, -1, null);
        } catch (DtmfCollectCommandException e) {
            assertTrue(e.getMessage().indexOf("Must specify") > -1);
            throw e;
        }
    }

    @Test
    public void testSetPromptFileUri() {
        // setup
        DtmfCollectCommand dtmfCollectCommand = new DtmfCollectCommand("myUri", true, true, 12, 34,
                56, 44);
        assertEquals("myUri", dtmfCollectCommand.getPromptFileUri());

        // act
        dtmfCollectCommand.setPromptFileUri("myOtherUri");

        // assert
        assertEquals("myOtherUri", dtmfCollectCommand.getPromptFileUri());
    }

    @Test
    public void testParseLengthPatternString() throws Exception {
    	// act
    	DtmfPattern p = DtmfCollectCommand.parseStringPattern("length=2");

    	// assert
    	assertEquals(2, ((DtmfLengthPattern)p).getLength());
	}

    @Test
    public void testParseLengthPatternWithCancelString() throws Exception {
    	// act
    	DtmfPattern p = DtmfCollectCommand.parseStringPattern("length=2;cancel=*");

    	// assert
    	assertEquals(2, ((DtmfLengthPattern)p).getLength());
    	assertEquals('*', ((DtmfLengthPattern)p).getCancelKey());
	}

    @Test(expected = NumberFormatException.class)
    public void testParseLengthPatternStringNoLength() throws Exception {
    	// act
    	DtmfCollectCommand.parseStringPattern("length=");
	}

    @Test
    public void testParseMinMaxRetString() throws Exception {
    	// act
    	DtmfPattern p = DtmfCollectCommand.parseStringPattern("min=2;max=4;rtk=#");

    	// assert
    	assertEquals(2, ((DtmfMinMaxRetPattern)p).getMinDigits());
    	assertEquals(4, ((DtmfMinMaxRetPattern)p).getMaxDigits());
    	assertEquals('#', ((DtmfMinMaxRetPattern)p).getReturnKey());
	}

    @Test
    public void testParseMinMaxRetCancelString() throws Exception {
    	// act
    	DtmfPattern p = DtmfCollectCommand.parseStringPattern("min=2;max=4;rtk=#;cancel=*");

    	// assert
    	assertEquals(2, ((DtmfMinMaxRetPattern)p).getMinDigits());
    	assertEquals(4, ((DtmfMinMaxRetPattern)p).getMaxDigits());
    	assertEquals('#', ((DtmfMinMaxRetPattern)p).getReturnKey());
    	assertEquals('*', ((DtmfMinMaxRetPattern)p).getCancelKey());
	}

    @Test
    public void testParseMinMaxRetStringNoRtkNoCancel() throws Exception {
    	// act
    	DtmfPattern p = DtmfCollectCommand.parseStringPattern("min=2;max=4");

    	// assert
    	assertEquals(2, ((DtmfMinMaxRetPattern)p).getMinDigits());
    	assertEquals(4, ((DtmfMinMaxRetPattern)p).getMaxDigits());
    	assertEquals(null, ((DtmfMinMaxRetPattern)p).getReturnKey());
    	assertEquals(null, ((DtmfMinMaxRetPattern)p).getCancelKey());
	}

    @Test
    public void testParseMinMaxRetStringNoMin() throws Exception {
    	// act
    	DtmfPattern p = DtmfCollectCommand.parseStringPattern("max=4;rtk=3");

    	// assert
    	assertEquals(1, ((DtmfMinMaxRetPattern)p).getMinDigits());
    	assertEquals(4, ((DtmfMinMaxRetPattern)p).getMaxDigits());
    	assertEquals('3', ((DtmfMinMaxRetPattern)p).getReturnKey());
	}
}
