// package com.angrysurfer.core.redis;

// import com.angrysurfer.core.sequencer.DrumSequenceData;
// import com.angrysurfer.core.sequencer.DrumSequencer;
// import com.angrysurfer.core.sequencer.MelodicSequenceData;
// import com.angrysurfer.core.sequencer.MelodicSequencer;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.Arrays;
// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class RedisServiceSequencerTest extends RedisServiceTest {

//     @Mock
//     private DrumSequencer mockDrumSequencer;

//     @Mock
//     private MelodicSequencer mockMelodicSequencer;

//     @Mock
//     private DrumSequenceData mockDrumSequenceData;

//     @Mock
//     private MelodicSequenceData mockMelodicSequenceData;

//     @BeforeEach
//     void setUpSequencerTests() {
//         // Additional setup for sequencer tests
//         when(mockDrumSequencer.getId()).thenReturn(1L);
//         when(mockMelodicSequencer.getId()).thenReturn(1);
//         when(mockMelodicSequenceData.getId()).thenReturn(1L);
//         when(mockMelodicSequencer.getSequenceData()).thenReturn(mockMelodicSequenceData);
//     }

//     @Test
//     void testFindDrumSequenceById() {
//         // Arrange
//         Long sequenceId = 1L;
//         when(getDrumSequenceHelper().findDrumSequenceById(sequenceId)).thenReturn(mockDrumSequenceData);

//         // Act
//         DrumSequenceData result = getRedisService().findDrumSequenceById(sequenceId);

//         // Assert
//         assertNotNull(result);
//         assertEquals(mockDrumSequenceData, result);
//     }

//     @Test
//     void testSaveDrumSequence() {
//         // Act
//         getRedisService().saveDrumSequence(mockDrumSequencer);

//         // Assert
//         verify(getDrumSequenceHelper()).saveDrumSequence(mockDrumSequencer);
//     }

//     @Test
//     void testFindMelodicSequenceById() {
//         // Arrange
//         Long sequenceId = 1L;
//         when(getMelodicSequencerHelper().
//
//         findMelodicSequenceById(sequenceId, 0))
//             .thenReturn(mockMelodicSequenceData);

//         // Act
//         MelodicSequenceData result = getRedisService().findMelodicSequenceById(sequenceId);

//         // Assert
//         assertNotNull(result);
//         assertEquals(mockMelodicSequenceData, result);
//     }

//     @Test
//     void testSaveMelodicSequence() {
//         // Act
//         getRedisService().saveMelodicSequence(mockMelodicSequencer);

//         // Assert
//         verify(getMelodicSequencerHelper()).saveMelodicSequence(mockMelodicSequencer);
//     }

//     @Test
//     void testGetAllMelodicSequenceIds() {
//         // Arrange
//         Integer sequencerId = 1;
//         List<Long> expectedIds = Arrays.asList(1L, 2L, 3L);
//         when(getMelodicSequencerHelper().getAllMelodicSequenceIds(sequencerId)).thenReturn(expectedIds);

//         // Act
//         List<Long> result = getRedisService().getAllMelodicSequenceIds(sequencerId);

//         // Assert
//         assertEquals(expectedIds, result);
//     }

//     @Test
//     void testApplyMelodicSequenceToSequencer() {
//         // Arrange
//         int[] tiltValues = new int[] {1, 2, 3, 4};
//         when(mockMelodicSequenceData.getHarmonicTiltValuesRaw()).thenReturn(tiltValues);
//         when(mockMelodicSequencer.isPlaying()).thenReturn(false);
//         when(mockMelodicSequencer.getCurrentStep()).thenReturn(0);

//         // Act
//         getRedisService().applyMelodicSequenceToSequencer(mockMelodicSequenceData, mockMelodicSequencer);

//         // Assert
//         verify(mockMelodicSequencer).setSequenceData(mockMelodicSequenceData);
//         verify(mockMelodicSequencer).updateQuantizer();
//         verify(mockMelodicSequenceData).getHarmonicTiltValuesRaw();
//     }
// }