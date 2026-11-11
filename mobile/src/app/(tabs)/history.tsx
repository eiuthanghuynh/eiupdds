import React, { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  RefreshControl,
  StyleSheet,
  View,
} from 'react-native';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import {
  Camera,
  ChevronRight,
  Clock,
  History,
  MapPin,
  Sparkles,
} from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { TitleComponent } from '@/components/common/TitleComponent';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { BottomTabInset, MaxContentWidth, Spacing } from '@/constants/theme';
import { useDiagnosis } from '@/context/DiagnosisContext';
import { useLanguage } from '@/context/LanguageContext';
import { useAppTheme } from '@/context/ThemeContext';
import { DiagnosisService } from '@/services/diagnosisService';
import { DiagnosisSummaryDto, SeverityLevel } from '@/types/diagnosis';

export default function HistoryScreen() {
  const { t, language } = useLanguage();
  const { colors, isDark } = useAppTheme();
  const { setCurrentDiagnosis, setSelectedPhotoUri } = useDiagnosis();

  const [items, setItems] = useState<DiagnosisSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadingDetailId, setLoadingDetailId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchHistory = useCallback(async () => {
    try {
      const response = await DiagnosisService.getHistory({ page: 0, size: 30 });
      setItems(response.content || []);
      setErrorMessage(null);
    } catch (err: any) {
      console.warn('Failed to fetch diagnosis history:', err);
      setErrorMessage(t('loadHistoryFailed'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [t]);

  const handleRefresh = useCallback(() => {
    setRefreshing(true);
    fetchHistory();
  }, [fetchHistory]);

  useEffect(() => {
    let ignore = false;

    DiagnosisService.getHistory({ page: 0, size: 30 })
      .then((response) => {
        if (!ignore) {
          setItems(response.content || []);
          setErrorMessage(null);
        }
      })
      .catch((err) => {
        console.warn('Failed to fetch diagnosis history:', err);
        if (!ignore) {
          setErrorMessage(t('loadHistoryFailed'));
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
          setRefreshing(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [t]);

  const handleOpenDetail = async (item: DiagnosisSummaryDto) => {
    if (loadingDetailId) return;

    try {
      setLoadingDetailId(item.diagnosisId);
      const detail = await DiagnosisService.getDiagnosisDetail(item.diagnosisId);
      setCurrentDiagnosis(detail);
      setSelectedPhotoUri(detail.originalImageUrl || item.originalImageUrl);
      router.push('/result' as any);
    } catch (err) {
      console.warn('Could not fetch diagnosis detail:', err);
      // Fallback: create partial response from summary
      if (item.dominantDisease) {
        setCurrentDiagnosis({
          diagnosisId: item.diagnosisId,
          originalImageUrl: item.originalImageUrl,
          imageWidth: 1024,
          imageHeight: 768,
          dominantDisease: item.dominantDisease,
          dominantConfidence: item.dominantConfidence,
          boundingBoxes: [],
          coOccurringDiseases: [],
          recommendedTreatments: [],
          inferenceTimeMs: 0,
          status: 'COMPLETED',
          diagnosedAt: item.createdAt,
        });
        setSelectedPhotoUri(item.originalImageUrl);
        router.push('/result' as any);
      }
    } finally {
      setLoadingDetailId(null);
    }
  };

  const getSeverityBadge = (level?: SeverityLevel) => {
    switch (level) {
      case 'CRITICAL':
        return {
          label: t('severityCritical'),
          bgColor: isDark ? 'rgba(239, 68, 68, 0.25)' : '#FEE2E2',
          textColor: '#EF4444',
          borderColor: '#EF4444',
        };
      case 'HIGH':
        return {
          label: t('severityHigh'),
          bgColor: isDark ? 'rgba(245, 158, 11, 0.25)' : '#FEF3C7',
          textColor: '#F59E0B',
          borderColor: '#F59E0B',
        };
      case 'MODERATE':
        return {
          label: t('severityModerate'),
          bgColor: isDark ? 'rgba(234, 179, 8, 0.25)' : '#FEF9C3',
          textColor: '#EAB308',
          borderColor: '#EAB308',
        };
      case 'LOW':
      default:
        return {
          label: t('severityLow'),
          bgColor: isDark ? 'rgba(34, 197, 94, 0.25)' : '#DCFCE7',
          textColor: '#22C55E',
          borderColor: '#22C55E',
        };
    }
  };

  const formatTimestamp = (isoDate?: string) => {
    if (!isoDate) return '';
    try {
      const date = new Date(isoDate);
      return date.toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoDate;
    }
  };

  const renderHistoryItem = ({ item }: { item: DiagnosisSummaryDto }) => {
    const isDetailLoading = loadingDetailId === item.diagnosisId;
    const diseaseName =
      language === 'vi'
        ? item.dominantDisease?.commonNameVi || item.dominantDisease?.diseaseCode || t('noDiseaseDetected')
        : item.dominantDisease?.commonNameEn || item.dominantDisease?.diseaseCode || t('noDiseaseDetected');

    const severityBadge = getSeverityBadge(item.dominantDisease?.severityLevel);
    const confidencePct = Math.round((item.dominantConfidence ?? 0) * 100);

    return (
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Xem kết quả chẩn đoán: ${diseaseName}`}
        style={({ pressed }) => [
          styles.card,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
          },
          pressed && styles.cardPressed,
        ]}
        onPress={() => handleOpenDetail(item)}>
        {/* Left: Thumbnail with Bounding Box Count Badge */}
        <View style={styles.thumbnailWrapper}>
          <Image
            source={{ uri: item.originalImageUrl }}
            style={styles.thumbnail}
            contentFit="cover"
            transition={200}
          />
          {item.boundingBoxCount > 0 && (
            <View style={styles.spotCountBadge}>
              <ThemedText style={styles.spotCountText}>
                {item.boundingBoxCount} {t('spotsCount')}
              </ThemedText>
            </View>
          )}
        </View>

        {/* Center: Info Content */}
        <View style={styles.cardContent}>
          {/* Top row: Plant name & Severity Badge */}
          <View style={styles.cardHeaderRow}>
            {item.plant ? (
              <ThemedText type="small" style={styles.plantName}>
                {language === 'vi' ? item.plant.commonNameVi : item.plant.commonNameEn}
              </ThemedText>
            ) : (
              <View />
            )}
            <View
              style={[
                styles.severityBadge,
                {
                  backgroundColor: severityBadge.bgColor,
                  borderColor: severityBadge.borderColor,
                },
              ]}>
              <ThemedText style={[styles.severityBadgeText, { color: severityBadge.textColor }]}>
                {severityBadge.label}
              </ThemedText>
            </View>
          </View>

          {/* Disease Common Name */}
          <ThemedText numberOfLines={2} style={[styles.diseaseNameText, { fontWeight: '700' }]}>
            {diseaseName}
          </ThemedText>

          {/* AI Confidence & Date */}
          <View style={styles.metaRow}>
            <View style={styles.confidenceWrap}>
              <Sparkles size={13} color="#22C55E" />
              <ThemedText type="small" style={styles.confidenceText}>
                {confidencePct}%
              </ThemedText>
            </View>

            <View style={styles.dateWrap}>
              <Clock size={12} color={colors.textSecondary} />
              <ThemedText type="small" style={[styles.dateText, { color: colors.textSecondary }]}>
                {formatTimestamp(item.createdAt)}
              </ThemedText>
            </View>
          </View>

          {/* Optional GPS coordinate indicator */}
          {item.latitude !== null && item.latitude !== undefined && (
            <View style={styles.gpsRow}>
              <MapPin size={11} color={colors.textSecondary} />
              <ThemedText type="small" style={[styles.gpsText, { color: colors.textSecondary }]}>
                {item.latitude.toFixed(4)}, {item.longitude?.toFixed(4)}
              </ThemedText>
            </View>
          )}
        </View>

        {/* Right Arrow / Loader */}
        <View style={styles.arrowWrap}>
          {isDetailLoading ? (
            <ActivityIndicator size="small" color={colors.primary} />
          ) : (
            <ChevronRight size={20} color={colors.textSecondary} />
          )}
        </View>
      </Pressable>
    );
  };

  const renderEmptyState = () => {
    if (loading) return null;

    return (
      <View style={styles.emptyContainer}>
        <View
          style={[
            styles.emptyIconCircle,
            { backgroundColor: isDark ? 'rgba(47, 58, 16, 0.4)' : '#EEF2DE' },
          ]}>
          <History size={40} color={colors.primary} />
        </View>
        <ThemedText type="subtitle" style={styles.emptyTitle}>
          {errorMessage || t('emptyHistoryTitle')}
        </ThemedText>
        <ThemedText style={[styles.emptySubtitle, { color: colors.textSecondary }]}>
          {errorMessage ? '' : t('emptyHistorySubtitle')}
        </ThemedText>

        <Pressable
          style={({ pressed }) => [
            styles.scanButton,
            { backgroundColor: colors.backgroundSelected },
            pressed && styles.buttonPressed,
          ]}
          onPress={() => router.push('/camera')}>
          <Camera size={18} color="#ffffff" />
          <ThemedText style={styles.scanButtonText}>{t('scanLeafNow')}</ThemedText>
        </Pressable>
      </View>
    );
  };

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        {/* Fixed Header Title - strictly complies with AGENTS.md rule */}
        <View style={styles.headerContainer}>
          <TitleComponent title={t('searchTitle')} subtitle={t('searchSubtitle')} />
        </View>

        {loading ? (
          <View style={styles.loadingCenter}>
            <ActivityIndicator size="large" color={colors.primary} />
            <ThemedText style={[styles.loadingText, { color: colors.textSecondary }]}>
              {t('loadingHistory')}
            </ThemedText>
          </View>
        ) : (
          <FlatList
            data={items}
            keyExtractor={(item) => item.diagnosisId}
            renderItem={renderHistoryItem}
            ListEmptyComponent={renderEmptyState}
            contentContainerStyle={styles.listContent}
            showsVerticalScrollIndicator={false}
            refreshControl={
              <RefreshControl
                refreshing={refreshing}
                onRefresh={handleRefresh}
                tintColor={colors.primary}
                colors={[colors.primary]}
              />
            }
          />
        )}
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    flexDirection: 'row',
  },
  safeArea: {
    flex: 1,
    paddingHorizontal: Spacing.four,
    paddingTop: Spacing.two,
    maxWidth: MaxContentWidth,
    width: '100%',
  },
  headerContainer: {
    width: '100%',
    paddingBottom: Spacing.two,
    zIndex: 10,
  },
  listContent: {
    paddingTop: Spacing.one,
    paddingBottom: BottomTabInset + Spacing.four,
  },
  loadingCenter: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: Spacing.two,
    fontSize: 14,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 14,
    borderWidth: 1,
    padding: Spacing.three,
    marginBottom: Spacing.three,
  },
  cardPressed: {
    opacity: 0.8,
    transform: [{ scale: 0.99 }],
  },
  thumbnailWrapper: {
    position: 'relative',
    width: 76,
    height: 76,
    borderRadius: 10,
    overflow: 'hidden',
  },
  thumbnail: {
    width: '100%',
    height: '100%',
  },
  spotCountBadge: {
    position: 'absolute',
    bottom: 3,
    left: 3,
    right: 3,
    backgroundColor: 'rgba(0, 0, 0, 0.65)',
    borderRadius: 4,
    paddingVertical: 1,
    alignItems: 'center',
  },
  spotCountText: {
    color: '#ffffff',
    fontSize: 9,
    fontWeight: '700',
  },
  cardContent: {
    flex: 1,
    marginLeft: Spacing.three,
    marginRight: Spacing.two,
  },
  cardHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 2,
  },
  plantName: {
    fontSize: 11,
    fontWeight: '600',
    textTransform: 'uppercase',
    color: '#16A34A',
  },
  severityBadge: {
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 6,
    borderWidth: 0.8,
  },
  severityBadgeText: {
    fontSize: 10,
    fontWeight: '700',
  },
  diseaseNameText: {
    fontSize: 15,
    lineHeight: 20,
    marginBottom: Spacing.one,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  confidenceWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  confidenceText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#22C55E',
  },
  dateWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  dateText: {
    fontSize: 11,
  },
  gpsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    marginTop: 3,
  },
  gpsText: {
    fontSize: 10,
  },
  arrowWrap: {
    justifyContent: 'center',
    alignItems: 'center',
    paddingLeft: Spacing.one,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.six,
    paddingHorizontal: Spacing.four,
  },
  emptyIconCircle: {
    width: 80,
    height: 80,
    borderRadius: 40,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.three,
  },
  emptyTitle: {
    textAlign: 'center',
    marginBottom: Spacing.one,
  },
  emptySubtitle: {
    textAlign: 'center',
    fontSize: 13,
    lineHeight: 18,
    marginBottom: Spacing.four,
  },
  scanButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    paddingVertical: 12,
    paddingHorizontal: Spacing.four,
    borderRadius: 10,
  },
  scanButtonText: {
    color: '#ffffff',
    fontWeight: '700',
    fontSize: 14,
  },
  buttonPressed: {
    opacity: 0.8,
  },
});
