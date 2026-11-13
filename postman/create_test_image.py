from PIL import Image

# Create a 200x200 green leaf sample image
img = Image.new('RGB', (200, 200), color=(34, 139, 34))
img.save('postman/test_leaf.jpg', 'JPEG')
print('Sample test_leaf.jpg generated successfully.')
