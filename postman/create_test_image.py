from PIL import Image

# Create a 200x200 blank green test image to exercise zero-detection retake errors (422 DIAGNOSIS_003)
img = Image.new('RGB', (200, 200), color=(34, 139, 34))
img.save('postman/blank_leaf.jpg', 'JPEG')
print('Sample blank_leaf.jpg generated successfully.')
