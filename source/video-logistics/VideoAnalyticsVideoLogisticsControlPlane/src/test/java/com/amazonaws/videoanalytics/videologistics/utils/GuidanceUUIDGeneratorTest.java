
package com.amazonaws.videoanalytics.videologistics.utils;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import static com.amazonaws.videoanalytics.videologistics.utils.TestConstants.EXPECTED_UUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GuidanceUUIDGeneratorTest {

    GuidanceUUIDGenerator guidanceUUIDGenerator;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void generateUUIDRandomTest(){

        guidanceUUIDGenerator = mock(GuidanceUUIDGenerator.class);
        when((guidanceUUIDGenerator.generateUUIDRandom())).thenReturn(EXPECTED_UUID);
        assertEquals(EXPECTED_UUID, guidanceUUIDGenerator.generateUUIDRandom());
    }
}