import React from 'react';
import { StyleSheet, View } from 'react-native';
import Svg, { Rect, Text as SvgText, G } from 'react-native-svg';
import { BoundingBoxDto } from '@/types/diagnosis';

interface BoundingBoxOverlayProps {
  boxes: BoundingBoxDto[];
  containerWidth: number;
  containerHeight: number;
}

export function BoundingBoxOverlay({
  boxes,
  containerWidth,
  containerHeight,
}: BoundingBoxOverlayProps) {
  if (!boxes || boxes.length === 0 || containerWidth <= 0 || containerHeight <= 0) {
    return null;
  }

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="none">
      <Svg width={containerWidth} height={containerHeight}>
        {boxes.map((box, index) => {
          // Normalize coordinates to container display bounds
          const x = (box.xMinNormalized ?? 0) * containerWidth;
          const y = (box.yMinNormalized ?? 0) * containerHeight;
          const width = (box.widthNormalized ?? 0) * containerWidth;
          const height = (box.heightNormalized ?? 0) * containerHeight;

          const confidencePct = Math.round((box.confidence ?? 0) * 100);
          const labelText = `${box.label || box.diseaseCode} (${confidencePct}%)`;

          // Severity color scheme
          const strokeColor = box.confidence > 0.8 ? '#EF4444' : '#F59E0B';
          const fillColor = box.confidence > 0.8 ? 'rgba(239, 68, 68, 0.18)' : 'rgba(245, 158, 11, 0.18)';

          const badgeWidth = Math.min(Math.max(labelText.length * 7.5 + 14, 90), containerWidth - x - 4);
          const badgeHeight = 22;
          const badgeY = Math.max(y - badgeHeight, 2);

          return (
            <G key={box.id || `box-${index}`}>
              {/* Detection bounding box with soft corner radius */}
              <Rect
                x={x}
                y={y}
                width={width}
                height={height}
                rx={6}
                ry={6}
                stroke={strokeColor}
                strokeWidth={2.5}
                fill={fillColor}
              />

              {/* Label Pill Background */}
              <Rect
                x={x}
                y={badgeY}
                width={badgeWidth}
                height={badgeHeight}
                rx={4}
                ry={4}
                fill={strokeColor}
              />

              {/* Label Text */}
              <SvgText
                x={x + 6}
                y={badgeY + 15}
                fill="#ffffff"
                fontSize={11}
                fontWeight="bold">
                {labelText}
              </SvgText>
            </G>
          );
        })}
      </Svg>
    </View>
  );
}
