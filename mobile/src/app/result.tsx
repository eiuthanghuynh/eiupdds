import React, { useState } from 'react';
import {
  LayoutChangeEvent,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import { Image } from 'expo-image';
import { router } from 'expo-router';
import {
  AlertTriangle,
  ArrowLeft,
  Camera,
  CheckCircle2,
  Clock,
  FlaskConical,
  Info,
  Leaf,
  ShieldCheck,
  Sparkles,
} from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { TitleComponent } from '@/components/common/TitleComponent';
import { BoundingBoxOverlay } from '@/components/diagnosis/BoundingBoxOverlay';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { BottomTabInset, MaxContentWidth, Spacing } from '@/constants/theme';
import { useDiagnosis } from '@/context/DiagnosisContext';
import { useLanguage } from '@/context/LanguageContext';
import { useAppTheme } from '@/context/ThemeContext';
import { SeverityLevel, TreatmentType } from '@/types/diagnosis';

export default function ResultScreen() {
  const { t } = useLanguage();
  const { colors, isDark } = useAppTheme();
  const { currentDiagnosis, selectedPhotoUri } = useDiagnosis();

  const [imageLayout, setImageLayout] = useState<{ width: number; height: number }>({
    width: 0,
    height: 0,
  });

  const handleImageLayout = (event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setImageLayout({ width, height });
  };

  const getSeverityBadge = (level?: SeverityLevel) => {
    switch (level) {
      case 'CRITICAL':
        return {
          label: t('severityCritical'),
          bgColor: '#FEE2E2',
          textColor: '#DC2626',
          borderColor: '#EF4444',
        };
      case 'HIGH':
        return {
          label: t('severityHigh'),
          bgColor: '#FEF3C7',
          textColor: '#D97706',
          borderColor: '#F59E0B',
        };
      case 'MODERATE':
        return {
          label: t('severityModerate'),
          bgColor: '#FEF9C3',
          textColor: '#CA8A04',
          borderColor: '#EAB308',
        };
      case 'LOW':
      default:
        return {
          label: t('severityLow'),
          bgColor: '#DCFCE7',
          textColor: '#15803D',
          borderColor: '#22C55E',
        };
    }
  };

  const getTreatmentIcon = (type: TreatmentType) => {
    switch (type) {
      case 'BIOLOGICAL':
        return <Leaf size={18} color="#10B981" />;
      case 'CHEMICAL':
        return <FlaskConical size={18} color="#EF4444" />;
      case 'PREVENTIVE':
        return <ShieldCheck size={18} color="#3B82F6" />;
      case 'CULTURAL':
      default:
        return <CheckCircle2 size={18} color="#F59E0B" />;
    }
  };

  const getTreatmentTypeName = (type: TreatmentType) => {
    switch (type) {
      case 'BIOLOGICAL':
        return t('treatmentBiological');
      case 'CHEMICAL':
        return t('treatmentChemical');
      case 'PREVENTIVE':
        return t('treatmentPreventive');
      case 'CULTURAL':
      default:
        return t('treatmentCultural');
    }
  };

  const displayImageUri = selectedPhotoUri || currentDiagnosis?.originalImageUrl;
  const severityBadge = getSeverityBadge(currentDiagnosis?.dominantDisease?.severityLevel);
  const confidencePercent = Math.round((currentDiagnosis?.dominantConfidence ?? 0) * 100);

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        {/* Fixed Header Title - Strictly according to AGENTS.md rule */}
        <View style={styles.headerContainer}>
          <View style={styles.topNavRow}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Quay lại máy ảnh"
              style={({ pressed }) => [
                styles.backButton,
                { backgroundColor: isDark ? '#243015' : '#E7EBD9', borderColor: colors.border },
                pressed && styles.pressed,
              ]}
              onPress={() => router.back()}>
              <ArrowLeft size={20} color={colors.text} />
            </Pressable>
            <View style={styles.headerTitleWrap}>
              <TitleComponent
                title={t('diagnosisResultTitle')}
                subtitle={t('diagnosisResultSubtitle')}
              />
            </View>
          </View>
        </View>

        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}>
          {/* 1. Leaf Image with Bounding Box Overlay */}
          <View style={[styles.imageCard, { borderColor: colors.border }]}>
            <View style={styles.imageWrapper} onLayout={handleImageLayout}>
              {displayImageUri ? (
                <Image
                  source={{ uri: displayImageUri }}
                  style={styles.leafImage}
                  contentFit="cover"
                />
              ) : (
                <View style={[styles.emptyImage, { backgroundColor: colors.surface }]}>
                  <ThemedText type="small" themeColor="textSecondary">
                    Không có ảnh mẫu
                  </ThemedText>
                </View>
              )}

              {/* Vector Bounding Box Overlay */}
              {currentDiagnosis?.boundingBoxes && (
                <BoundingBoxOverlay
                  boxes={currentDiagnosis.boundingBoxes}
                  containerWidth={imageLayout.width}
                  containerHeight={imageLayout.height}
                />
              )}
            </View>

            <View style={styles.imageFooter}>
              <View style={styles.boxCountBadge}>
                <Sparkles size={14} color="#CFFF74" />
                <ThemedText style={styles.boxCountText}>
                  {currentDiagnosis?.boundingBoxes?.length || 0} vùng phát hiện AI
                </ThemedText>
              </View>
              {currentDiagnosis?.inferenceTimeMs && (
                <View style={styles.timeBadge}>
                  <Clock size={12} color={colors.textSecondary} />
                  <ThemedText type="small" themeColor="textSecondary">
                    {currentDiagnosis.inferenceTimeMs}ms
                  </ThemedText>
                </View>
              )}
            </View>
          </View>

          {/* 2. Dominant Disease Card */}
          {currentDiagnosis?.dominantDisease ? (
            <ThemedView
              type="surface"
              style={[styles.card, { borderColor: colors.border }]}>
              <View style={styles.cardHeaderRow}>
                <View style={styles.diseaseNameWrap}>
                  <ThemedText type="title" style={styles.diseaseCommonName}>
                    {currentDiagnosis.dominantDisease.commonNameVi}
                  </ThemedText>
                  <ThemedText
                    type="small"
                    style={styles.scientificName}
                    themeColor="textSecondary">
                    {currentDiagnosis.dominantDisease.scientificName}
                  </ThemedText>
                </View>

                <View
                  style={[
                    styles.severityPill,
                    {
                      backgroundColor: severityBadge.bgColor,
                      borderColor: severityBadge.borderColor,
                    },
                  ]}>
                  <ThemedText
                    style={[styles.severityPillText, { color: severityBadge.textColor }]}>
                    {severityBadge.label}
                  </ThemedText>
                </View>
              </View>

              {/* Confidence Progress Bar */}
              <View style={styles.confidenceSection}>
                <View style={styles.confidenceLabelRow}>
                  <ThemedText type="smallBold">{t('confidenceScore')}</ThemedText>
                  <ThemedText type="smallBold" style={{ color: '#10B981' }}>
                    {confidencePercent}%
                  </ThemedText>
                </View>
                <View style={[styles.progressTrack, { backgroundColor: colors.border }]}>
                  <View
                    style={[
                      styles.progressFill,
                      {
                        width: `${confidencePercent}%`,
                        backgroundColor:
                          confidencePercent > 80
                            ? '#10B981'
                            : confidencePercent > 60
                            ? '#F59E0B'
                            : '#EF4444',
                      },
                    ]}
                  />
                </View>
              </View>

              {/* Symptoms Description */}
              {currentDiagnosis.dominantDisease.symptomsVi && (
                <View style={styles.symptomsBox}>
                  <Info size={16} color={colors.text} />
                  <ThemedText type="small" style={styles.symptomsText}>
                    {currentDiagnosis.dominantDisease.symptomsVi}
                  </ThemedText>
                </View>
              )}
            </ThemedView>
          ) : (
            <ThemedView
              type="surface"
              style={[styles.card, { borderColor: colors.border }]}>
              <ThemedText>{t('noDiseaseDetected')}</ThemedText>
            </ThemedView>
          )}

          {/* 3. Co-occurring Diseases (if any) */}
          {currentDiagnosis?.coOccurringDiseases &&
            currentDiagnosis.coOccurringDiseases.length > 0 && (
              <View style={styles.sectionWrap}>
                <ThemedText type="smallBold" style={styles.sectionTitle}>
                  {t('coOccurringDiseases')}
                </ThemedText>
                {currentDiagnosis.coOccurringDiseases.map((coDisease) => (
                  <ThemedView
                    key={coDisease.id}
                    type="surface"
                    style={[styles.coDiseaseCard, { borderColor: colors.border }]}>
                    <AlertTriangle size={18} color="#F59E0B" />
                    <View style={styles.coDiseaseInfo}>
                      <ThemedText type="smallBold">{coDisease.commonNameVi}</ThemedText>
                      <ThemedText type="small" themeColor="textSecondary">
                        {coDisease.scientificName}
                      </ThemedText>
                    </View>
                  </ThemedView>
                ))}
              </View>
            )}

          {/* 4. Recommended Treatments Section */}
          <View style={styles.sectionWrap}>
            <ThemedText type="title" style={styles.sectionTitle}>
              {t('recommendedTreatments')}
            </ThemedText>

            {currentDiagnosis?.recommendedTreatments &&
            currentDiagnosis.recommendedTreatments.length > 0 ? (
              currentDiagnosis.recommendedTreatments.map((treatment) => (
                <ThemedView
                  key={treatment.id}
                  type="surface"
                  style={[styles.treatmentCard, { borderColor: colors.border }]}>
                  <View style={styles.treatmentHeader}>
                    <View style={styles.treatmentTypePill}>
                      {getTreatmentIcon(treatment.treatmentType)}
                      <ThemedText type="smallBold" style={styles.treatmentTypeName}>
                        {getTreatmentTypeName(treatment.treatmentType)}
                      </ThemedText>
                    </View>
                    {treatment.safetyIntervalDays !== undefined &&
                      treatment.safetyIntervalDays !== null && (
                        <ThemedText type="small" themeColor="textSecondary">
                          Cách ly: {treatment.safetyIntervalDays} {t('days')}
                        </ThemedText>
                      )}
                  </View>

                  <ThemedText type="smallBold" style={styles.treatmentTitle}>
                    {treatment.titleVi}
                  </ThemedText>

                  <ThemedText type="small" style={styles.treatmentContent}>
                    {treatment.contentVi}
                  </ThemedText>

                  {treatment.activeIngredients && (
                    <View style={styles.metaRow}>
                      <ThemedText type="smallBold" style={styles.metaKey}>
                        {t('activeIngredients')}{' '}
                      </ThemedText>
                      <ThemedText type="small" themeColor="textSecondary">
                        {treatment.activeIngredients}
                      </ThemedText>
                    </View>
                  )}

                  {treatment.dosageInstructions && (
                    <View style={styles.metaRow}>
                      <ThemedText type="smallBold" style={styles.metaKey}>
                        {t('dosage')}{' '}
                      </ThemedText>
                      <ThemedText type="small" themeColor="textSecondary">
                        {treatment.dosageInstructions}
                      </ThemedText>
                    </View>
                  )}
                </ThemedView>
              ))
            ) : (
              <ThemedView
                type="surface"
                style={[styles.card, { borderColor: colors.border }]}>
                <ThemedText type="small" themeColor="textSecondary">
                  Không có phác đồ bổ sung cho mẫu này.
                </ThemedText>
              </ThemedView>
            )}
          </View>

          {/* Action Button: Scan another leaf */}
          <Pressable
            style={({ pressed }) => [
              styles.actionButton,
              { backgroundColor: colors.backgroundSelected },
              pressed && styles.pressed,
            ]}
            onPress={() => router.push('/camera')}>
            <Camera size={20} color="#ffffff" />
            <ThemedText style={styles.actionButtonText}>{t('scanAnother')}</ThemedText>
          </Pressable>
        </ScrollView>
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
  topNavRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
  },
  headerTitleWrap: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
    width: '100%',
  },
  scrollContent: {
    paddingTop: Spacing.one,
    paddingBottom: BottomTabInset + Spacing.five,
    gap: Spacing.three,
  },
  imageCard: {
    borderRadius: Spacing.three,
    overflow: 'hidden',
    borderWidth: 1,
    backgroundColor: '#000000',
  },
  imageWrapper: {
    width: '100%',
    height: 260,
    position: 'relative',
    backgroundColor: '#1E293B',
  },
  leafImage: {
    width: '100%',
    height: '100%',
  },
  emptyImage: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  imageFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    backgroundColor: '#18200E',
  },
  boxCountBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  boxCountText: {
    color: '#CFFF74',
    fontSize: 12,
    fontWeight: '700',
  },
  timeBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  card: {
    borderRadius: Spacing.four,
    padding: Spacing.three,
    gap: Spacing.three,
    borderWidth: 1,
  },
  cardHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  diseaseNameWrap: {
    flex: 1,
    gap: 2,
    paddingRight: Spacing.two,
  },
  diseaseCommonName: {
    fontSize: 18,
    lineHeight: 22,
  },
  scientificName: {
    fontStyle: 'italic',
    fontSize: 13,
  },
  severityPill: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    borderWidth: 1,
  },
  severityPillText: {
    fontSize: 11,
    fontWeight: '800',
  },
  confidenceSection: {
    gap: 6,
  },
  confidenceLabelRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  progressTrack: {
    height: 8,
    borderRadius: 4,
    overflow: 'hidden',
    width: '100%',
  },
  progressFill: {
    height: '100%',
    borderRadius: 4,
  },
  symptomsBox: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
    backgroundColor: 'rgba(47, 58, 16, 0.08)',
    padding: Spacing.two,
    borderRadius: Spacing.two,
  },
  symptomsText: {
    flex: 1,
    lineHeight: 18,
  },
  sectionWrap: {
    gap: Spacing.two,
    marginTop: Spacing.two,
  },
  sectionTitle: {
    fontSize: 17,
  },
  coDiseaseCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    padding: Spacing.two,
    borderRadius: Spacing.three,
    borderWidth: 1,
  },
  coDiseaseInfo: {
    flex: 1,
  },
  treatmentCard: {
    borderRadius: Spacing.three,
    padding: Spacing.three,
    gap: Spacing.two,
    borderWidth: 1,
  },
  treatmentHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  treatmentTypePill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  treatmentTypeName: {
    fontSize: 13,
  },
  treatmentTitle: {
    fontSize: 15,
  },
  treatmentContent: {
    lineHeight: 19,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  metaKey: {
    fontSize: 12,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 14,
    borderRadius: 14,
    marginTop: Spacing.two,
  },
  actionButtonText: {
    color: '#ffffff',
    fontWeight: '700',
    fontSize: 15,
  },
  pressed: {
    opacity: 0.75,
  },
});
