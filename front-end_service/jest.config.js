module.exports = {
       testEnvironment: 'jest-environment-jsdom',
       setupFilesAfterEnv: ['<rootDir>/jest.setup.js','<rootDir>/src/setupTests.ts'],
       moduleNameMapper: {
         '\\.(css|less|scss|sass)$': 'identity-obj-proxy',
       },
       transform: {
         '^.+\\.(ts|tsx|js|jsx)$': ['babel-jest', {
           presets: [
             ['@babel/preset-env', { targets: { node: 'current' } }],
             ['@babel/preset-react', { runtime: 'automatic' }],
             '@babel/preset-typescript'
           ]
         }]
       },
       extensionsToTreatAsEsm: ['.ts', '.tsx'],
     };