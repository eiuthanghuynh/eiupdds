import React, { useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  StyleSheet,
  View,
} from 'react-native';
import {
  CameraType,
  CameraView,
  FlashMode,
  useCameraPermissions,
} from 'expo-camera';
import { Image } from 'expo-image';
import * as ImagePicker from 'expo-image-picker';
import { router, useIsFocused } from 'expo-router';
import {
  Camera,
  Images,
  RotateCcw,
  Sparkles,
  SwitchCamera,
  Zap,
  ZapOff,
} from 'lucide-react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { BottomTabInset, Spacing } from '@/constants/theme';
import { useDiagnosis } from '@/context/DiagnosisContext';

// Safe optional module import to prevent crash if native module ExpoLocation is not compiled in current binary
let LocationModule: typeof import('expo-location') | null = null;
try {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  LocationModule = require('expo-location');
} catch {
  // ExpoLocation native module not present in current binary runtime
}

export interface PhotoPreviewData {
  uri: string;
  width?: number;
  height?: number;
  source: 'camera' | 'gallery';
}

export default function CameraScreen() {
  const { analyzePhoto, isAnalyzing } = useDiagnosis();
  const [facing, setFacing] = useState<CameraType>('back');
  const [flash, setFlash] = useState<FlashMode>('off');
  const [photo, setPhoto] = useState<PhotoPreviewData | null>(null);
  const [isTakingPhoto, setIsTakingPhoto] = useState(false);
  const [confirmed, setConfirmed] = useState(false);

  const cameraRef = useRef<CameraView>(null);
  const isFocused = useIsFocused();
  const insets = useSafeAreaInsets();
  const [permission, requestPermission] = useCameraPermissions();

  const toggleFacing = () => {
    setFacing((current) => (current === 'back' ? 'front' : 'back'));
  };

  const toggleFlash = () => {
    setFlash((current) => {
      if (current === 'off') return 'on';
      if (current === 'on') return 'auto';
      return 'off';
    });
  };

  const handleTakePicture = async () => {
    if (!cameraRef.current || isTakingPhoto) return;

    try {
      setIsTakingPhoto(true);
      const picture = await cameraRef.current.takePictureAsync({
        quality: 0.85,
        skipProcessing: false,
      });

      if (picture) {
        setPhoto({
          uri: picture.uri,
          width: picture.width,
          height: picture.height,
          source: 'camera',
        });
        setConfirmed(false);
      }
    } catch (error) {
      console.error('Error taking picture:', error);
      Alert.alert(
        'Không thể chụp ảnh',
        'Đã có lỗi xảy ra khi chụp ảnh. Vui lòng thử lại.'
      );
    } finally {
      setIsTakingPhoto(false);
    }
  };

  const handlePickImage = async () => {
    try {
      const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!permissionResult.granted) {
        Alert.alert(
          'Cần quyền truy cập thư viện',
          'Vui lòng cấp quyền truy cập thư viện ảnh để chọn hình cây trồng cần chẩn đoán.'
        );
        return;
      }

      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsEditing: true,
        quality: 0.85,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        const selected = result.assets[0];
        setPhoto({
          uri: selected.uri,
          width: selected.width,
          height: selected.height,
          source: 'gallery',
        });
        setConfirmed(false);
      }
    } catch (error) {
      console.error('Error picking image from library:', error);
      Alert.alert('Lỗi', 'Không thể chọn ảnh từ thư viện. Vui lòng thử lại.');
    }
  };

  const handleRetake = () => {
    setPhoto(null);
    setConfirmed(false);
  };

  const handleConfirmPhoto = async () => {
    if (!photo || isAnalyzing) return;
    try {
      setConfirmed(true);

      // Attempt to retrieve device GPS location with timeout protection (if native module available)
      let latitude: number | undefined;
      let longitude: number | undefined;

      if (LocationModule?.requestForegroundPermissionsAsync) {
        try {
          const { status } = await LocationModule.requestForegroundPermissionsAsync();
          if (status === 'granted') {
            const loc = await Promise.race([
              LocationModule.getCurrentPositionAsync({
                accuracy: LocationModule.Accuracy.Balanced,
              }),
              new Promise<null>((resolve) => setTimeout(() => resolve(null), 3000)),
            ]);
            if (loc && 'coords' in loc) {
              latitude = loc.coords.latitude;
              longitude = loc.coords.longitude;
            }
          }
        } catch (locErr) {
          console.warn('GPS location request failed or was skipped:', locErr);
        }
      }

      await analyzePhoto(photo.uri, { latitude, longitude });
      router.push('/result' as any);
    } catch (error: any) {
      setConfirmed(false);
      Alert.alert(
        'Lỗi chẩn đoán',
        error?.message || 'Không thể thực hiện chẩn đoán lúc này. Vui lòng thử lại.'
      );
    }
  };

  // 1. Loading permission state
  if (!permission) {
    return (
      <ThemedView style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#2F3A10" />
        <ThemedText style={styles.loadingText}>Đang chuẩn bị máy ảnh...</ThemedText>
      </ThemedView>
    );
  }

  // 2. Permission not granted state
  if (!permission.granted) {
    return (
      <ThemedView style={styles.centerContainer}>
        <View style={styles.permissionCard}>
          <View style={styles.permissionIconCircle}>
            <Camera size={44} color="#2F3A10" />
          </View>
          <ThemedText type="title" style={styles.permissionTitle}>
            Cần quyền truy cập máy ảnh
          </ThemedText>
          <ThemedText style={styles.permissionSubtitle}>
            Ứng dụng cần quyền sử dụng máy ảnh để chụp hình cây trồng và chẩn đoán sâu bệnh hại.
          </ThemedText>

          <Pressable
            style={({ pressed }) => [
              styles.permissionButton,
              pressed && styles.buttonPressed,
            ]}
            onPress={requestPermission}>
            <ThemedText style={styles.permissionButtonText}>
              Cấp quyền máy ảnh
            </ThemedText>
          </Pressable>
        </View>
      </ThemedView>
    );
  }

  // Bottom padding to ensure controls sit nicely above the bottom tab bar
  const bottomControlsPadding = Math.max(
    insets.bottom + BottomTabInset + Spacing.two,
    BottomTabInset + Spacing.four
  );

  // 3. Captured photo preview state
  if (photo) {
    return (
      <View style={styles.container}>
        <Image
          source={{ uri: photo.uri }}
          style={StyleSheet.absoluteFill}
          contentFit="cover"
        />

        {/* Top header on preview */}
        <View style={[styles.previewHeader, { paddingTop: insets.top + Spacing.two }]}>
          <View style={styles.badge}>
            <Sparkles size={16} color="#ffffff" />
            <ThemedText style={styles.badgeText}>
              {confirmed
                ? 'Đã xác nhận ảnh'
                : photo.source === 'gallery'
                ? 'Xem trước ảnh từ thư viện'
                : 'Xem trước ảnh chụp'}
            </ThemedText>
          </View>
        </View>

        {/* Loading overlay during AI inference */}
        {isAnalyzing && (
          <View style={styles.loadingOverlay}>
            <ActivityIndicator size="large" color="#CFFF74" />
            <ThemedText style={styles.loadingOverlayTitle}>Đang phân tích hình ảnh...</ThemedText>
            <ThemedText style={styles.loadingOverlaySubtitle}>
              Mô hình Computer Vision đang quét vết bệnh và thiết lập phác đồ
            </ThemedText>
          </View>
        )}

        {/* Bottom actions on preview */}
        <View style={[styles.previewActions, { paddingBottom: bottomControlsPadding }]}>
          <Pressable
            disabled={isAnalyzing}
            style={({ pressed }) => [
              styles.actionButton,
              styles.retakeButton,
              pressed && styles.buttonPressed,
              isAnalyzing && { opacity: 0.5 },
            ]}
            onPress={handleRetake}>
            <RotateCcw size={20} color="#ffffff" />
            <ThemedText style={styles.actionButtonText}>
              {photo.source === 'gallery' ? 'Chọn ảnh khác' : 'Chụp lại'}
            </ThemedText>
          </Pressable>

          <Pressable
            disabled={isAnalyzing}
            style={({ pressed }) => [
              styles.actionButton,
              styles.confirmButton,
              pressed && styles.buttonPressed,
              isAnalyzing && { opacity: 0.7 },
            ]}
            onPress={handleConfirmPhoto}>
            {isAnalyzing ? (
              <ActivityIndicator size="small" color="#ffffff" />
            ) : (
              <Sparkles size={20} color="#ffffff" />
            )}
            <ThemedText style={styles.actionButtonText}>
              {isAnalyzing ? 'Đang phân tích...' : 'Chẩn đoán ngay'}
            </ThemedText>
          </Pressable>
        </View>
      </View>
    );
  }

  // 4. Live camera feed
  return (
    <View style={styles.container}>
      {/* Unmount CameraView when screen is not focused to prevent hardware conflicts and save battery */}
      {isFocused ? (
        <CameraView
          ref={cameraRef}
          facing={facing}
          flash={flash}
          mode="picture"
          style={StyleSheet.absoluteFill}
        />
      ) : (
        <View style={styles.blackBackground} />
      )}

      {/* Top control bar */}
      <View style={[styles.topBar, { paddingTop: insets.top + Spacing.two }]}>
        <View style={styles.badge}>
          <ThemedText style={styles.badgeText}>Nhận diện bệnh cây</ThemedText>
        </View>

        <View style={styles.topActions}>
          {/* Flash toggle */}
          <Pressable
            style={({ pressed }) => [
              styles.circleButton,
              flash !== 'off' && styles.circleButtonActive,
              pressed && styles.buttonPressed,
            ]}
            onPress={toggleFlash}>
            {flash === 'off' ? (
              <ZapOff size={20} color="#ffffff" />
            ) : (
              <Zap size={20} color="#ffffff" />
            )}
            {flash === 'auto' && (
              <ThemedText style={styles.flashAutoBadge}>A</ThemedText>
            )}
          </Pressable>

          {/* Switch facing */}
          <Pressable
            style={({ pressed }) => [
              styles.circleButton,
              pressed && styles.buttonPressed,
            ]}
            onPress={toggleFacing}>
            <SwitchCamera size={20} color="#ffffff" />
          </Pressable>
        </View>
      </View>

      {/* Viewfinder overlay */}
      <View style={styles.viewfinderContainer} pointerEvents="none">
        <View style={styles.viewfinderFrame}>
          {/* 4 corner brackets */}
          <View style={[styles.corner, styles.cornerTopLeft]} />
          <View style={[styles.corner, styles.cornerTopRight]} />
          <View style={[styles.corner, styles.cornerBottomLeft]} />
          <View style={[styles.corner, styles.cornerBottomRight]} />
        </View>
        <ThemedText style={styles.viewfinderHint}>
          Đặt lá hoặc vùng bệnh của cây vào trong khung
        </ThemedText>
      </View>

      {/* Bottom controls */}
      <View style={[styles.bottomBar, { paddingBottom: bottomControlsPadding }]}>
        <View style={styles.bottomBarInner}>
          {/* Gallery Button */}
          <Pressable
            accessibilityLabel="Chọn ảnh từ thư viện"
            style={({ pressed }) => [
              styles.secondaryButton,
              pressed && styles.buttonPressed,
            ]}
            onPress={handlePickImage}>
            <Images size={24} color="#ffffff" />
          </Pressable>

          {/* Shutter Button */}
          <Pressable
            accessibilityLabel="Chụp ảnh"
            disabled={isTakingPhoto}
            style={({ pressed }) => [
              styles.shutterOuter,
              pressed && styles.shutterPressed,
            ]}
            onPress={handleTakePicture}>
            <View style={styles.shutterInner}>
              {isTakingPhoto && (
                <ActivityIndicator size="small" color="#2F3A10" />
              )}
            </View>
          </Pressable>

          {/* Flip camera */}
          <Pressable
            accessibilityLabel="Lật máy ảnh"
            style={({ pressed }) => [
              styles.secondaryButton,
              pressed && styles.buttonPressed,
            ]}
            onPress={toggleFacing}>
            <SwitchCamera size={24} color="#ffffff" />
          </Pressable>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  blackBackground: {
    flex: 1,
    backgroundColor: '#000000',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.four,
  },
  loadingText: {
    marginTop: Spacing.three,
    fontSize: 15,
  },
  permissionCard: {
    alignItems: 'center',
    padding: Spacing.four,
    maxWidth: 360,
    width: '100%',
  },
  permissionIconCircle: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: 'rgba(47, 58, 16, 0.12)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.three,
  },
  permissionTitle: {
    textAlign: 'center',
    marginBottom: Spacing.two,
  },
  permissionSubtitle: {
    textAlign: 'center',
    opacity: 0.75,
    lineHeight: 20,
    marginBottom: Spacing.four,
  },
  permissionButton: {
    backgroundColor: '#2F3A10',
    paddingVertical: 14,
    paddingHorizontal: Spacing.four,
    borderRadius: 12,
    width: '100%',
    alignItems: 'center',
  },
  permissionButtonText: {
    color: '#ffffff',
    fontWeight: '700',
    fontSize: 16,
  },
  buttonPressed: {
    opacity: 0.75,
  },
  topBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.four,
    zIndex: 10,
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
  },
  badgeText: {
    color: '#ffffff',
    fontSize: 13,
    fontWeight: '600',
  },
  topActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  circleButton: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
  },
  circleButtonActive: {
    backgroundColor: '#2F3A10',
    borderColor: '#CFFF74',
  },
  flashAutoBadge: {
    position: 'absolute',
    top: 2,
    right: 4,
    fontSize: 10,
    fontWeight: '800',
    color: '#ffffff',
  },
  viewfinderContainer: {
    ...StyleSheet.absoluteFill,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 5,
  },
  viewfinderFrame: {
    width: 270,
    height: 270,
    position: 'relative',
    borderRadius: 16,
  },
  corner: {
    position: 'absolute',
    width: 32,
    height: 32,
    borderColor: '#ffffff',
  },
  cornerTopLeft: {
    top: 0,
    left: 0,
    borderTopWidth: 3.5,
    borderLeftWidth: 3.5,
    borderTopLeftRadius: 12,
  },
  cornerTopRight: {
    top: 0,
    right: 0,
    borderTopWidth: 3.5,
    borderRightWidth: 3.5,
    borderTopRightRadius: 12,
  },
  cornerBottomLeft: {
    bottom: 0,
    left: 0,
    borderBottomWidth: 3.5,
    borderLeftWidth: 3.5,
    borderBottomLeftRadius: 12,
  },
  cornerBottomRight: {
    bottom: 0,
    right: 0,
    borderBottomWidth: 3.5,
    borderRightWidth: 3.5,
    borderBottomRightRadius: 12,
  },
  viewfinderHint: {
    marginTop: Spacing.three,
    color: '#ffffff',
    fontSize: 13,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 14,
    textAlign: 'center',
    maxWidth: 290,
  },
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    zIndex: 10,
    alignItems: 'center',
  },
  bottomBarInner: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    width: '100%',
    maxWidth: 380,
    paddingHorizontal: Spacing.three,
  },
  secondaryButton: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.25)',
  },
  shutterOuter: {
    width: 78,
    height: 78,
    borderRadius: 39,
    borderWidth: 4,
    borderColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
  },
  shutterPressed: {
    transform: [{ scale: 0.94 }],
  },
  shutterInner: {
    width: 62,
    height: 62,
    borderRadius: 31,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  previewHeader: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    alignItems: 'center',
    zIndex: 10,
  },
  previewActions: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'center',
    gap: Spacing.three,
    paddingHorizontal: Spacing.four,
    zIndex: 10,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 14,
    paddingHorizontal: Spacing.four,
    borderRadius: 14,
    minWidth: 140,
  },
  retakeButton: {
    backgroundColor: 'rgba(30, 30, 30, 0.85)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.25)',
  },
  confirmButton: {
    backgroundColor: '#10B981',
  },
  actionButtonText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '700',
  },
  loadingOverlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(0, 0, 0, 0.72)',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 20,
    paddingHorizontal: Spacing.four,
    gap: Spacing.two,
  },
  loadingOverlayTitle: {
    color: '#ffffff',
    fontSize: 17,
    fontWeight: '700',
    marginTop: Spacing.two,
    textAlign: 'center',
  },
  loadingOverlaySubtitle: {
    color: 'rgba(255, 255, 255, 0.75)',
    fontSize: 13,
    textAlign: 'center',
    maxWidth: 280,
  },
});
